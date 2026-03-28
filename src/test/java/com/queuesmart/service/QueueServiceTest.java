package com.queuesmart.service;

import com.queuesmart.dto.QueueDto;
import com.queuesmart.model.*;
import com.queuesmart.repository.HistoryRepository;
import com.queuesmart.repository.QueueRepository;
import com.queuesmart.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock private QueueRepository queueRepository;
    @Mock private UserRepository userRepository;
    @Mock private ServiceManagementService serviceManagementService;
    @Mock private WaitTimeEstimator waitTimeEstimator;
    @Mock private NotificationService notificationService;
    @Mock private HistoryRepository historyRepository;

    @InjectMocks private QueueService queueService;

    private User testUser;
    private com.queuesmart.model.Service testService;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-1")
                .username("alice")
                .email("alice@example.com")
                .role(User.Role.USER)
                .build();

        testService = com.queuesmart.model.Service.builder()
                .id("svc-1")
                .name("Advising")
                .description("Academic advising")
                .expectedDurationMinutes(15)
                .priorityLevel(com.queuesmart.model.Service.PriorityLevel.MEDIUM)
                .active(true)
                .build();
    }

    // ---- JOIN QUEUE ----

    @Test
    void joinQueue_Success_ReturnsQueueEntry() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(serviceManagementService.getRawService("svc-1")).thenReturn(testService);
        when(queueRepository.findByUserIdAndServiceId("user-1", "svc-1")).thenReturn(Optional.empty());
        when(queueRepository.findActiveByServiceId("svc-1")).thenReturn(List.of());
        when(waitTimeEstimator.estimate(anyInt(), anyInt(), any())).thenReturn(0);
        when(queueRepository.save(any(QueueEntry.class))).thenAnswer(i -> i.getArgument(0));

        QueueDto.QueueEntryResponse response = queueService.joinQueue("user-1", "svc-1", null);

        assertNotNull(response);
        assertEquals("user-1", response.getUserId());
        assertEquals("svc-1", response.getServiceId());
        assertEquals(QueueEntry.QueueStatus.WAITING, response.getStatus());
        verify(notificationService).sendQueueJoined(eq("user-1"), eq("Advising"), anyInt());
    }

    @Test
    void joinQueue_InactiveService_ThrowsException() {
        testService.setActive(false);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(serviceManagementService.getRawService("svc-1")).thenReturn(testService);

        assertThrows(IllegalArgumentException.class,
                () -> queueService.joinQueue("user-1", "svc-1", null));
    }

    @Test
    void joinQueue_AlreadyInQueue_ThrowsException() {
        QueueEntry existing = QueueEntry.builder()
                .id("entry-1").userId("user-1").serviceId("svc-1")
                .status(QueueEntry.QueueStatus.WAITING).build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(serviceManagementService.getRawService("svc-1")).thenReturn(testService);
        when(queueRepository.findByUserIdAndServiceId("user-1", "svc-1")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> queueService.joinQueue("user-1", "svc-1", null));
    }

    @Test
    void joinQueue_UserNotFound_ThrowsException() {
        when(userRepository.findById("bad")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> queueService.joinQueue("bad", "svc-1", null));
    }

    // ---- LEAVE QUEUE ----

    @Test
    void leaveQueue_Success_UpdatesStatusAndNotifies() {
        QueueEntry entry = QueueEntry.builder()
                .id("entry-1").userId("user-1").serviceId("svc-1")
                .joinedAt(LocalDateTime.now().minusMinutes(5))
                .status(QueueEntry.QueueStatus.WAITING).build();

        when(queueRepository.findByUserIdAndServiceId("user-1", "svc-1")).thenReturn(Optional.of(entry));
        when(serviceManagementService.getRawService("svc-1")).thenReturn(testService);
        when(queueRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(queueRepository.findActiveByServiceId("svc-1")).thenReturn(List.of());

        queueService.leaveQueue("user-1", "svc-1");

        assertEquals(QueueEntry.QueueStatus.LEFT, entry.getStatus());
        verify(notificationService).sendQueueLeft("user-1", "Advising");
        verify(historyRepository).save(any(HistoryRecord.class));
    }

    @Test
    void leaveQueue_NotInQueue_ThrowsException() {
        when(queueRepository.findByUserIdAndServiceId("user-1", "svc-1")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> queueService.leaveQueue("user-1", "svc-1"));
    }

    // ---- SERVE NEXT ----

    @Test
    void serveNext_Success_ServesFirstEntry() {
        QueueEntry entry = QueueEntry.builder()
                .id("entry-1").userId("user-1").serviceId("svc-1").username("alice")
                .joinedAt(LocalDateTime.now().minusMinutes(10))
                .status(QueueEntry.QueueStatus.WAITING)
                .priorityLevel(com.queuesmart.model.Service.PriorityLevel.MEDIUM).build();

        when(queueRepository.findActiveByServiceId("svc-1")).thenReturn(List.of(entry));
        when(serviceManagementService.getRawService("svc-1")).thenReturn(testService);
        when(queueRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        // After serving, second call for recalculate returns empty
        when(queueRepository.findActiveByServiceId("svc-1"))
                .thenReturn(List.of(entry))
                .thenReturn(List.of());

        QueueDto.QueueEntryResponse response = queueService.serveNext("svc-1");

        assertNotNull(response);
        verify(notificationService).sendYourTurn("user-1", "Advising");
        verify(historyRepository).save(any(HistoryRecord.class));
    }

    @Test
    void serveNext_EmptyQueue_ThrowsException() {
        when(queueRepository.findActiveByServiceId("svc-1")).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> queueService.serveNext("svc-1"));
    }

    // ---- GET STATUS ----

    @Test
    void getQueueStatus_ReturnsCorrectStatus() {
        QueueEntry entry = QueueEntry.builder()
                .id("e1").userId("user-1").serviceId("svc-1")
                .status(QueueEntry.QueueStatus.WAITING)
                .priorityLevel(com.queuesmart.model.Service.PriorityLevel.MEDIUM)
                .joinedAt(LocalDateTime.now()).build();

        when(serviceManagementService.getRawService("svc-1")).thenReturn(testService);
        when(queueRepository.findActiveByServiceId("svc-1")).thenReturn(List.of(entry));
        when(waitTimeEstimator.estimate(anyInt(), anyInt(), any())).thenReturn(5);
        when(waitTimeEstimator.estimateForNewUser(anyInt(), anyInt())).thenReturn(15);

        QueueDto.QueueStatusResponse status = queueService.getQueueStatus("svc-1");

        assertEquals("svc-1", status.getServiceId());
        assertEquals(1, status.getTotalWaiting());
        assertEquals("Advising", status.getServiceName());
    }
}
