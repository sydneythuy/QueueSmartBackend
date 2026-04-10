package com.queuesmart.service;

import com.queuesmart.dto.QueueDto;
import com.queuesmart.model.*;
import com.queuesmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueRepository          queueRepository;
    private final QueueEntryRepository     entryRepository;
    private final UserCredentialRepository credentialRepository;
    private final ServiceManagementService serviceManagementService;
    private final WaitTimeEstimator        waitTimeEstimator;
    private final NotificationService      notificationService;
    private final HistoryRecordRepository  historyRepo;
    private final UserProfileRepository    profileRepository;

    @Transactional
    public QueueDto.QueueEntryResponse joinQueue(String userId, String serviceId,
                                                  com.queuesmart.model.Service.PriorityLevel priority) {
        UserCredential user = credentialRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        com.queuesmart.model.Service service = serviceManagementService.getRawService(serviceId);
        if (!service.isActive()) throw new IllegalArgumentException("This service is currently not active");

        Queue queue = queueRepository.findByServiceId(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found for this service"));

        if (queue.getStatus() == Queue.QueueStatus.CLOSED)
            throw new IllegalArgumentException("This queue is currently closed");

        // Prevent duplicate active entries
        entryRepository.findByQueue_IdAndUser_IdAndStatus(queue.getId(), userId,
                QueueEntry.EntryStatus.WAITING)
                .ifPresent(e -> { throw new IllegalArgumentException("You are already in this queue"); });

        com.queuesmart.model.Service.PriorityLevel effectivePriority = priority != null ? priority : service.getPriorityLevel();

        List<QueueEntry> activeEntries = entryRepository.findActiveByQueueIdOrdered(queue.getId());
        int position = activeEntries.size() + 1;
        int wait = waitTimeEstimator.estimate(position, service.getExpectedDurationMinutes(), effectivePriority);

        QueueEntry entry = QueueEntry.builder()
                .id(UUID.randomUUID().toString())
                .queue(queue)
                .user(user)
                .position(position)
                .joinedAt(LocalDateTime.now())
                .status(QueueEntry.EntryStatus.WAITING)
                .priorityLevel(effectivePriority)
                .estimatedWaitMinutes(wait)
                .build();
        entryRepository.save(entry);

        notificationService.sendQueueJoined(userId, service.getName(), position);
        return toResponse(entry, usernameOf(user));
    }

    @Transactional
    public void leaveQueue(String userId, String serviceId) {
        Queue queue = queueRepository.findByServiceId(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found"));

        QueueEntry entry = entryRepository.findByQueue_IdAndUser_IdAndStatus(
                        queue.getId(), userId, QueueEntry.EntryStatus.WAITING)
                .orElseThrow(() -> new IllegalArgumentException("You are not in this queue"));

        entry.setStatus(QueueEntry.EntryStatus.LEFT);
        entryRepository.save(entry);

        com.queuesmart.model.Service service = serviceManagementService.getRawService(serviceId);
        saveHistory(entry, service, QueueEntry.EntryStatus.LEFT);
        notificationService.sendQueueLeft(userId, service.getName());
        recalculatePositions(queue.getId(), service.getExpectedDurationMinutes());
    }

    @Transactional(readOnly = true)
    public QueueDto.QueueStatusResponse getQueueStatus(String serviceId) {
        com.queuesmart.model.Service service = serviceManagementService.getRawService(serviceId);
        Queue queue = queueRepository.findByServiceId(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found"));

        List<QueueEntry> active = entryRepository.findActiveByQueueIdOrdered(queue.getId());
        for (int i = 0; i < active.size(); i++) {
            QueueEntry e = active.get(i);
            e.setPosition(i + 1);
            e.setEstimatedWaitMinutes(waitTimeEstimator.estimate(
                    i + 1, service.getExpectedDurationMinutes(), e.getPriorityLevel()));
        }

        QueueDto.QueueStatusResponse resp = new QueueDto.QueueStatusResponse();
        resp.setServiceId(serviceId);
        resp.setServiceName(service.getName());
        resp.setTotalWaiting(active.size());
        resp.setEstimatedWaitForNew(
                waitTimeEstimator.estimateForNewUser(active.size(), service.getExpectedDurationMinutes()));
        resp.setEntries(active.stream()
                .map(e -> toResponse(e, usernameOf(e.getUser())))
                .collect(Collectors.toList()));
        return resp;
    }

    @Transactional
    public QueueDto.QueueEntryResponse serveNext(String serviceId) {
        Queue queue = queueRepository.findByServiceId(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found"));

        List<QueueEntry> active = entryRepository.findActiveByQueueIdOrdered(queue.getId());
        if (active.isEmpty()) throw new IllegalArgumentException("Queue is empty");

        QueueEntry next = active.get(0);
        next.setStatus(QueueEntry.EntryStatus.SERVED);
        entryRepository.save(next);

        com.queuesmart.model.Service service = serviceManagementService.getRawService(serviceId);
        saveHistory(next, service, QueueEntry.EntryStatus.SERVED);
        notificationService.sendYourTurn(next.getUser().getId(), service.getName());

        recalculatePositions(queue.getId(), service.getExpectedDurationMinutes());

        // Notify new first-in-line if they're almost up
        List<QueueEntry> remaining = entryRepository.findActiveByQueueIdOrdered(queue.getId());
        if (!remaining.isEmpty() && remaining.get(0).getPosition() <= 2) {
            QueueEntry almostNext = remaining.get(0);
            notificationService.sendAlmostYourTurn(
                    almostNext.getUser().getId(), service.getName(), almostNext.getPosition());
        }
        return toResponse(next, usernameOf(next.getUser()));
    }

    @Transactional(readOnly = true)
    public QueueDto.QueueEntryResponse getUserQueueEntry(String userId, String serviceId) {
        Queue queue = queueRepository.findByServiceId(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found"));

        QueueEntry entry = entryRepository.findByQueue_IdAndUser_IdAndStatus(
                        queue.getId(), userId, QueueEntry.EntryStatus.WAITING)
                .orElseThrow(() -> new IllegalArgumentException("You are not in this queue"));

        com.queuesmart.model.Service service = serviceManagementService.getRawService(serviceId);
        List<QueueEntry> active = entryRepository.findActiveByQueueIdOrdered(queue.getId());
        int pos = active.indexOf(entry) + 1;
        entry.setPosition(pos);
        entry.setEstimatedWaitMinutes(waitTimeEstimator.estimate(
                pos, service.getExpectedDurationMinutes(), entry.getPriorityLevel()));

        return toResponse(entry, usernameOf(entry.getUser()));
    }

    // ── private helpers ───────────────────────────────────────

    @Transactional
    protected void recalculatePositions(String queueId, int durationMinutes) {
        List<QueueEntry> active = entryRepository.findActiveByQueueIdOrdered(queueId);
        for (int i = 0; i < active.size(); i++) {
            QueueEntry e = active.get(i);
            e.setPosition(i + 1);
            e.setEstimatedWaitMinutes(waitTimeEstimator.estimate(
                    i + 1, durationMinutes, e.getPriorityLevel()));
            entryRepository.save(e);
        }
    }

    private void saveHistory(QueueEntry entry, com.queuesmart.model.Service service,
                             QueueEntry.EntryStatus status) {
        long waited = ChronoUnit.MINUTES.between(entry.getJoinedAt(), LocalDateTime.now());
        HistoryRecord record = HistoryRecord.builder()
                .id(UUID.randomUUID().toString())
                .user(entry.getUser())
                .serviceId(service.getId())
                .serviceName(service.getName())
                .joinedAt(entry.getJoinedAt())
                .completedAt(LocalDateTime.now())
                .finalStatus(status)
                .waitedMinutes((int) Math.max(0, waited))
                .build();
        historyRepo.save(record);
    }

    private String usernameOf(UserCredential user) {
        return profileRepository.findByCredentialId(user.getId())
                .map(UserProfile::getUsername)
                .orElse(user.getEmail());
    }

    private QueueDto.QueueEntryResponse toResponse(QueueEntry e, String username) {
        QueueDto.QueueEntryResponse r = new QueueDto.QueueEntryResponse();
        r.setId(e.getId());
        r.setUserId(e.getUser().getId());
        r.setUsername(username);
        r.setServiceId(e.getQueue().getService().getId());
        r.setPosition(e.getPosition());
        r.setEstimatedWaitMinutes(e.getEstimatedWaitMinutes());
        r.setJoinedAt(e.getJoinedAt());
        r.setStatus(e.getStatus());
        r.setPriorityLevel(e.getPriorityLevel());
        return r;
    }
}







