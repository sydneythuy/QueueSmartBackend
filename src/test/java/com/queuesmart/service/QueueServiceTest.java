package com.queuesmart.service;

import com.queuesmart.dto.QueueDto;
import com.queuesmart.model.*;
import com.queuesmart.repository.*;
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

    @Mock private QueueRepository          queueRepository;
    @Mock private QueueEntryRepository     entryRepository;
    @Mock private UserCredentialRepository credentialRepository;
    @Mock private ServiceManagementService serviceManagementService;
    @Mock private WaitTimeEstimator        waitTimeEstimator;
    @Mock private NotificationService      notificationService;
    @Mock private HistoryRecordRepository  historyRepo;
    @Mock private UserProfileRepository    profileRepository;

    @InjectMocks private QueueService queueService;

    private UserCredential testUser;
    private Service        testService;
    private Queue          testQueue;

    @BeforeEach
    void setUp() {
        testUser = UserCredential.builder()
                .id("u1").email("alice@example.com").role(UserCredential.Role.USER).build();

        testService = Service.builder()
                .id("svc-1").name("Advising").description("Academic advising")
                .expectedDurationMinutes(15).priorityLevel(Service.PriorityLevel.MEDIUM)
                .active(true).build();

        testQueue = Queue.builder()
                .id("q1").service(testService).status(Queue.QueueStatus.OPEN).build();
    }

    // ── JOIN ──────────────────────────────────────────────────

    @Test
    void joinQueue_Success_CreatesEntryAndNotifies() {
        when(credentialRepository.findById("u1")).thenReturn(Optional.of(testUser));
        when(serviceManagementService.getRawService("svc-1")).thenReturn(testService);
        when(queueRepository.findByServiceId("svc-1")).thenReturn(Optional.of(testQueue));
        when(entryRepository.findByQueueIdAndUserIdAndStatus("q1", "u1",
                QueueEntry.EntryStatus.WAITING)).thenReturn(Optional.empty());
        when(entryRepository.findActiveByQueueIdOrdered("q1")).thenReturn(List.of());
        when(waitTimeEstimator.estimate(anyInt(), anyInt(), any())).thenReturn(0);
        when(entryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.findByCredentialId("u1")).thenReturn(Optional.empty());

        QueueDto.QueueEntryResponse resp = queueService.joinQueue("u1", "svc-1", null);

        assertNotNull(resp);
        assertEquals("u1", resp.getUserId());
        assertEquals(QueueEntry.EntryStatus.WAITING, resp.getStatus());
        verify(notificationService).sendQueueJoined(eq("u1"), eq("Advising"), anyInt());
    }

    @Test
    void joinQueue_InactiveService_ThrowsException() {
        testService.setActive(false);
        when(credentialRepository.findById("u1")).thenReturn(Optional.of(testUser));
        when(serviceManagementService.getRawService("svc-1")).thenReturn(testService);

        assertThrows(IllegalArgumentException.class,
                () -> queueService.joinQueue("u1", "svc-1", null));
    }

    @Test
    void joinQueue_ClosedQueue_ThrowsException() {
        testQueue.setStatus(Queue.QueueStatus.CLOSED);
        when(credentialRepository.findById("u1")).thenReturn(Optional.of(testUser));
        when(serviceManagementService.getRawService("svc-1")).thenReturn(testService);
        when(queueRepository.findByServiceId("svc-1")).thenReturn(Optional.of(testQueue));

        assertThrows(IllegalArgumentException.class,
                () -> queueService.joinQueue("u1", "svc-1", null));
    }

    @Test
    void joinQueue_AlreadyInQueue_ThrowsException() {
        QueueEntry existing = QueueEntry.builder().id("e1").build();
        when(credentialRepository.findById("u1")).thenReturn(Optional.of(testUser));
        when(serviceManagementService.getRawService("svc-1")).thenReturn(testService);
        when(queueRepository.findByServiceId("svc-1")).thenReturn(Optional.of(testQueue));
        when(entryRepository.findByQueueIdAndUserIdAndStatus("q1", "u1",
                QueueEntry.EntryStatus.WAITING)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> queueService.joinQueue("u1", "svc-1", null));
    }

    @Test
    void joinQueue_UserNotFound_ThrowsException() {
        when(credentialRepository.findById("bad")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> queueService.joinQueue("bad", "svc-1", null));
    }

    // ── LEAVE ─────────────────────────────────────────────────

    @Test
    void leaveQueue_Success_SetsLeftStatusAndNotifies() {
        QueueEntry entry = QueueEntry.builder()
                .id("e1").queue(testQueue).user(testUser)
                .joinedAt(LocalDateTime.now().minusMinutes(5))
                .status(QueueEntry.EntryStatus.WAITING)
                .priorityLevel(Service.PriorityLevel.MEDIUM).build();

        when(queueRepository.findByServiceId("svc-1")).thenReturn(Optional.of(testQueue));
        when(entryRepository.findByQueueIdAndUserIdAndStatus("q1", "u1",
                QueueEntry.EntryStatus.WAITING)).thenReturn(Optional.of(entry));
        when(entryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(serviceManagementService.getRawService("svc-1")).thenReturn(testService);
        when(entryRepository.findActiveByQueueIdOrdered("q1")).thenReturn(List.of());

        queueService.leaveQueue("u1", "svc-1");

        assertEquals(QueueEntry.EntryStatus.LEFT, entry.getStatus());
        verify(notificationService).sendQueueLeft("u1", "Advising");
        verify(historyRepo).save(any(HistoryRecord.class));
    }

    @Test
    void leaveQueue_NotInQueue_ThrowsException() {
        when(queueRepository.findByServiceId("svc-1")).thenReturn(Optional.of(testQueue));
        when(entryRepository.findByQueueIdAndUserIdAndStatus(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> queueService.leaveQueue("u1", "svc-1"));
    }

    // ── SERVE NEXT ────────────────────────────────────────────

    @Test
    void serveNext_Success_MarksServedAndNotifies() {
        QueueEntry entry = QueueEntry.builder()
                .id("e1").queue(testQueue).user(testUser)
                .joinedAt(LocalDateTime.now().minusMinutes(10))
                .status(QueueEntry.EntryStatus.WAITING)
                .priorityLevel(Service.PriorityLevel.MEDIUM).build();

        when(queueRepository.findByServiceId("svc-1")).thenReturn(Optional.of(testQueue));
        when(entryRepository.findActiveByQueueIdOrdered("q1"))
                .thenReturn(List.of(entry))   // first call — get next
                .thenReturn(List.of());        // second call — recalculate
        when(entryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(serviceManagementService.getRawService("svc-1")).thenReturn(testService);
        when(profileRepository.findByCredentialId("u1")).thenReturn(Optional.empty());

        QueueDto.QueueEntryResponse resp = queueService.serveNext("svc-1");

        assertNotNull(resp);
        verify(notificationService).sendYourTurn("u1", "Advising");
        verify(historyRepo).save(any(HistoryRecord.class));
    }

    @Test
    void serveNext_EmptyQueue_ThrowsException() {
        when(queueRepository.findByServiceId("svc-1")).thenReturn(Optional.of(testQueue));
        when(entryRepository.findActiveByQueueIdOrdered("q1")).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> queueService.serveNext("svc-1"));
    }

    // ── STATUS ────────────────────────────────────────────────

    @Test
    void getQueueStatus_ReturnsCorrectTotals() {
        QueueEntry entry = QueueEntry.builder()
                .id("e1").queue(testQueue).user(testUser)
                .status(QueueEntry.EntryStatus.WAITING)
                .priorityLevel(Service.PriorityLevel.MEDIUM)
                .joinedAt(LocalDateTime.now()).build();

        when(serviceManagementService.getRawService("svc-1")).thenReturn(testService);
        when(queueRepository.findByServiceId("svc-1")).thenReturn(Optional.of(testQueue));
        when(entryRepository.findActiveByQueueIdOrdered("q1")).thenReturn(List.of(entry));
        when(waitTimeEstimator.estimate(anyInt(), anyInt(), any())).thenReturn(15);
        when(waitTimeEstimator.estimateForNewUser(anyInt(), anyInt())).thenReturn(15);
        when(profileRepository.findByCredentialId("u1")).thenReturn(Optional.empty());

        QueueDto.QueueStatusResponse status = queueService.getQueueStatus("svc-1");

        assertEquals(1, status.getTotalWaiting());
        assertEquals("Advising", status.getServiceName());
        assertEquals(1, status.getEntries().size());
    }
}
