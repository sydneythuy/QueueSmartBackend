package com.queuesmart.service;

import com.queuesmart.dto.QueueDto;
import com.queuesmart.model.QueueEntry;
import com.queuesmart.repository.QueueRepository;
import com.queuesmart.repository.UserRepository;
import com.queuesmart.repository.HistoryRepository;
import com.queuesmart.model.HistoryRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueRepository queueRepository;
    private final UserRepository userRepository;
    private final ServiceManagementService serviceManagementService;
    private final WaitTimeEstimator waitTimeEstimator;
    private final NotificationService notificationService;
    private final HistoryRepository historyRepository;

    public QueueDto.QueueEntryResponse joinQueue(String userId, String serviceId, com.queuesmart.model.Service.PriorityLevel priority) {
        // Check user exists
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check service exists and is active
        com.queuesmart.model.Service service = serviceManagementService.getRawService(serviceId);
        if (!service.isActive()) {
            throw new IllegalArgumentException("This service is currently not active");
        }

        // Check user not already in this queue
        if (queueRepository.findByUserIdAndServiceId(userId, serviceId).isPresent()) {
            throw new IllegalArgumentException("You are already in the queue for this service");
        }

        // Resolve priority: use user's requested or fall back to service's priority
        com.queuesmart.model.Service.PriorityLevel effectivePriority = (priority != null) ? priority : service.getPriorityLevel();

        // Get current sorted active queue to determine position
        List<QueueEntry> activeQueue = queueRepository.findActiveByServiceId(serviceId);
        int position = activeQueue.size() + 1;

        int estimatedWait = waitTimeEstimator.estimate(position,
                service.getExpectedDurationMinutes(), effectivePriority);

        QueueEntry entry = QueueEntry.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .username(user.getUsername())
                .serviceId(serviceId)
                .position(position)
                .estimatedWaitMinutes(estimatedWait)
                .joinedAt(LocalDateTime.now())
                .status(QueueEntry.QueueStatus.WAITING)
                .priorityLevel(effectivePriority)
                .build();

        queueRepository.save(entry);

        // Notify user they have joined
        notificationService.sendQueueJoined(userId, service.getName(), position);

        return new QueueDto.QueueEntryResponse(entry);
    }

    public void leaveQueue(String userId, String serviceId) {
        QueueEntry entry = queueRepository.findByUserIdAndServiceId(userId, serviceId)
                .orElseThrow(() -> new IllegalArgumentException("You are not in this queue"));

        entry.setStatus(QueueEntry.QueueStatus.LEFT);
        queueRepository.save(entry);

        com.queuesmart.model.Service service = serviceManagementService.getRawService(serviceId);

        // Record in history
        saveHistory(entry, service, QueueEntry.QueueStatus.LEFT);

        // Notify
        notificationService.sendQueueLeft(userId, service.getName());

        // Recalculate positions for remaining users
        recalculatePositions(serviceId);
    }

    public QueueDto.QueueStatusResponse getQueueStatus(String serviceId) {
        com.queuesmart.model.Service service = serviceManagementService.getRawService(serviceId);
        List<QueueEntry> activeQueue = queueRepository.findActiveByServiceId(serviceId);

        // Update positions dynamically
        for (int i = 0; i < activeQueue.size(); i++) {
            activeQueue.get(i).setPosition(i + 1);
            activeQueue.get(i).setEstimatedWaitMinutes(
                    waitTimeEstimator.estimate(i + 1,
                            service.getExpectedDurationMinutes(),
                            activeQueue.get(i).getPriorityLevel()));
        }

        QueueDto.QueueStatusResponse response = new QueueDto.QueueStatusResponse();
        response.setServiceId(serviceId);
        response.setServiceName(service.getName());
        response.setTotalWaiting(activeQueue.size());
        response.setEstimatedWaitForNew(
                waitTimeEstimator.estimateForNewUser(activeQueue.size(), service.getExpectedDurationMinutes()));
        response.setEntries(activeQueue.stream()
                .map(QueueDto.QueueEntryResponse::new)
                .collect(Collectors.toList()));
        return response;
    }

    public QueueDto.QueueEntryResponse serveNext(String serviceId) {
        List<QueueEntry> activeQueue = queueRepository.findActiveByServiceId(serviceId);

        if (activeQueue.isEmpty()) {
            throw new IllegalArgumentException("Queue is empty — no one to serve");
        }

        QueueEntry next = activeQueue.get(0);
        next.setStatus(QueueEntry.QueueStatus.SERVING);
        queueRepository.save(next);

        com.queuesmart.model.Service service = serviceManagementService.getRawService(serviceId);

        // Mark as served immediately (no real serving session in this design)
        next.setStatus(QueueEntry.QueueStatus.SERVED);
        queueRepository.save(next);

        // Record history
        saveHistory(next, service, QueueEntry.QueueStatus.SERVED);

        // Notify the served user
        notificationService.sendYourTurn(next.getUserId(), service.getName());

        // Notify next-in-line if almost their turn (position 1 or 2 after serving)
        recalculatePositions(serviceId);
        List<QueueEntry> remaining = queueRepository.findActiveByServiceId(serviceId);
        if (!remaining.isEmpty() && remaining.get(0).getPosition() <= 2) {
            QueueEntry almostNext = remaining.get(0);
            notificationService.sendAlmostYourTurn(
                    almostNext.getUserId(), service.getName(), almostNext.getPosition());
        }

        return new QueueDto.QueueEntryResponse(next);
    }

    public QueueDto.QueueEntryResponse getUserQueueEntry(String userId, String serviceId) {
        QueueEntry entry = queueRepository.findByUserIdAndServiceId(userId, serviceId)
                .orElseThrow(() -> new IllegalArgumentException("You are not in this queue"));

        // Refresh position and wait
        com.queuesmart.model.Service service = serviceManagementService.getRawService(serviceId);
        List<QueueEntry> activeQueue = queueRepository.findActiveByServiceId(serviceId);
        int position = activeQueue.indexOf(entry) + 1;
        entry.setPosition(position);
        entry.setEstimatedWaitMinutes(
                waitTimeEstimator.estimate(position, service.getExpectedDurationMinutes(), entry.getPriorityLevel()));

        return new QueueDto.QueueEntryResponse(entry);
    }

    // ---- private helpers ----

    private void recalculatePositions(String serviceId) {
        com.queuesmart.model.Service service = serviceManagementService.getRawService(serviceId);
        List<QueueEntry> activeQueue = queueRepository.findActiveByServiceId(serviceId);
        for (int i = 0; i < activeQueue.size(); i++) {
            QueueEntry e = activeQueue.get(i);
            e.setPosition(i + 1);
            e.setEstimatedWaitMinutes(
                    waitTimeEstimator.estimate(i + 1, service.getExpectedDurationMinutes(), e.getPriorityLevel()));
            queueRepository.save(e);
        }
    }

    private void saveHistory(QueueEntry entry, com.queuesmart.model.Service service, QueueEntry.QueueStatus status) {
        long waited = ChronoUnit.MINUTES.between(entry.getJoinedAt(), LocalDateTime.now());
        HistoryRecord record = HistoryRecord.builder()
                .id(UUID.randomUUID().toString())
                .userId(entry.getUserId())
                .username(entry.getUsername())
                .serviceId(service.getId())
                .serviceName(service.getName())
                .joinedAt(entry.getJoinedAt())
                .completedAt(LocalDateTime.now())
                .finalStatus(status)
                .waitedMinutes((int) waited)
                .build();
        historyRepository.save(record);
    }
}
