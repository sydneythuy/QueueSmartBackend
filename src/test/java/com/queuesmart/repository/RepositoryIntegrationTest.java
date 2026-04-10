package com.queuesmart.repository;

import com.queuesmart.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Spring Data JPA repositories.
 * Uses H2 in-memory database (no real MySQL required).
 * Schema is auto-created from @Entity classes via ddl-auto=create-drop.
 */
@DataJpaTest
@ActiveProfiles("test")
class RepositoryIntegrationTest {

    @Autowired private UserCredentialRepository credentialRepo;
    @Autowired private UserProfileRepository    profileRepo;
    @Autowired private ServiceRepository        serviceRepo;
    @Autowired private QueueRepository          queueRepo;
    @Autowired private QueueEntryRepository     entryRepo;
    @Autowired private NotificationRepository   notifRepo;
    @Autowired private HistoryRecordRepository  historyRepo;

    private UserCredential savedUser;
    private Service        savedService;
    private Queue          savedQueue;

    @BeforeEach
    void setUp() {
        // Clean slate
        entryRepo.deleteAll();
        historyRepo.deleteAll();
        notifRepo.deleteAll();
        queueRepo.deleteAll();
        serviceRepo.deleteAll();
        profileRepo.deleteAll();
        credentialRepo.deleteAll();

        savedUser = credentialRepo.save(UserCredential.builder()
                .id("u1").email("alice@example.com")
                .password("$2a$hashed").role(UserCredential.Role.USER).build());

        savedService = serviceRepo.save(Service.builder()
                .id("s1").name("Advising").description("Academic advising")
                .expectedDurationMinutes(15).priorityLevel(Service.PriorityLevel.MEDIUM)
                .active(true).build());

        savedQueue = queueRepo.save(Queue.builder()
                .id("q1").service(savedService).status(Queue.QueueStatus.OPEN).build());
    }

    // ── UserCredentialRepository ──────────────────────────────

    @Test
    void credential_FindByEmail_ReturnsUser() {
        assertTrue(credentialRepo.findByEmail("alice@example.com").isPresent());
        assertFalse(credentialRepo.findByEmail("other@example.com").isPresent());
    }

    @Test
    void credential_ExistsByEmail_ReturnsTrue() {
        assertTrue(credentialRepo.existsByEmail("alice@example.com"));
        assertFalse(credentialRepo.existsByEmail("nope@example.com"));
    }

    @Test
    void credential_DuplicateEmail_ThrowsDataIntegrityViolation() {
        // unique constraint on email
        assertThrows(Exception.class, () ->
                credentialRepo.saveAndFlush(UserCredential.builder()
                        .id("u2").email("alice@example.com")
                        .password("hashed").role(UserCredential.Role.USER).build()));
    }

    // ── UserProfileRepository ─────────────────────────────────

    @Test
    void profile_SaveAndFindByCredentialId() {
        profileRepo.save(UserProfile.builder()
                .id("p1").credential(savedUser).username("alice")
                .emailVerified(false).build());

        assertTrue(profileRepo.findByCredentialId("u1").isPresent());
        assertEquals("alice", profileRepo.findByCredentialId("u1").get().getUsername());
    }

    @Test
    void profile_ExistsByUsername_CaseSensitive() {
        profileRepo.save(UserProfile.builder()
                .id("p1").credential(savedUser).username("Alice")
                .emailVerified(false).build());

        assertTrue(profileRepo.existsByUsername("Alice"));
        assertFalse(profileRepo.existsByUsername("alice")); // case-sensitive in H2
    }

    // ── ServiceRepository ─────────────────────────────────────

    @Test
    void service_FindAllByActiveTrue_FiltersInactive() {
        serviceRepo.save(Service.builder()
                .id("s2").name("IT Support").description("IT help")
                .expectedDurationMinutes(10).priorityLevel(Service.PriorityLevel.LOW)
                .active(false).build());

        List<Service> active = serviceRepo.findAllByActiveTrue();
        assertEquals(1, active.size());
        assertEquals("Advising", active.get(0).getName());
    }

    @Test
    void service_ExistsByNameIgnoreCase_ReturnsTrue() {
        assertTrue(serviceRepo.existsByNameIgnoreCase("advising"));
        assertTrue(serviceRepo.existsByNameIgnoreCase("ADVISING"));
        assertFalse(serviceRepo.existsByNameIgnoreCase("Clinic"));
    }

    // ── QueueRepository ───────────────────────────────────────

    @Test
    void queue_FindByServiceId_ReturnsQueue() {
        assertTrue(queueRepo.findByServiceId("s1").isPresent());
        assertFalse(queueRepo.findByServiceId("bad").isPresent());
    }

    // ── QueueEntryRepository ──────────────────────────────────

    @Test
    void entry_FindActiveByQueueIdOrdered_ReturnsOnlyWaiting() {
        entryRepo.save(QueueEntry.builder()
                .id("e1").queue(savedQueue).user(savedUser)
                .position(1).joinedAt(LocalDateTime.now())
                .status(QueueEntry.EntryStatus.WAITING)
                .priorityLevel(Service.PriorityLevel.MEDIUM).build());
        entryRepo.save(QueueEntry.builder()
                .id("e2").queue(savedQueue).user(savedUser)
                .position(2).joinedAt(LocalDateTime.now())
                .status(QueueEntry.EntryStatus.SERVED)
                .priorityLevel(Service.PriorityLevel.MEDIUM).build());

        List<QueueEntry> active = entryRepo.findActiveByQueueIdOrdered("q1");
        assertEquals(1, active.size());
        assertEquals("e1", active.get(0).getId());
    }

    @Test
    void entry_PriorityOrdering_HighBeforeLow() {
        // Create a second user for this test
        UserCredential user2 = credentialRepo.save(UserCredential.builder()
                .id("u2").email("bob@example.com")
                .password("hashed").role(UserCredential.Role.USER).build());

        LocalDateTime now = LocalDateTime.now();

        entryRepo.save(QueueEntry.builder()
                .id("e-low").queue(savedQueue).user(savedUser)
                .position(1).joinedAt(now)
                .status(QueueEntry.EntryStatus.WAITING)
                .priorityLevel(Service.PriorityLevel.LOW).build());

        entryRepo.save(QueueEntry.builder()
                .id("e-high").queue(savedQueue).user(user2)
                .position(2).joinedAt(now.plusSeconds(1))
                .status(QueueEntry.EntryStatus.WAITING)
                .priorityLevel(Service.PriorityLevel.HIGH).build());

        List<QueueEntry> ordered = entryRepo.findActiveByQueueIdOrdered("q1");
        assertEquals("e-high", ordered.get(0).getId()); // HIGH comes first
        assertEquals("e-low",  ordered.get(1).getId());
    }

    @Test
    void entry_CountByQueueIdAndStatus_IsCorrect() {
        entryRepo.save(QueueEntry.builder()
                .id("e1").queue(savedQueue).user(savedUser)
                .position(1).joinedAt(LocalDateTime.now())
                .status(QueueEntry.EntryStatus.WAITING)
                .priorityLevel(Service.PriorityLevel.MEDIUM).build());

        assertEquals(1, entryRepo.countByQueueIdAndStatus("q1", QueueEntry.EntryStatus.WAITING));
        assertEquals(0, entryRepo.countByQueueIdAndStatus("q1", QueueEntry.EntryStatus.SERVED));
    }

    @Test
    void entry_FindByQueueIdAndUserIdAndStatus_ReturnsCorrectEntry() {
        entryRepo.save(QueueEntry.builder()
                .id("e1").queue(savedQueue).user(savedUser)
                .position(1).joinedAt(LocalDateTime.now())
                .status(QueueEntry.EntryStatus.WAITING)
                .priorityLevel(Service.PriorityLevel.MEDIUM).build());

        assertTrue(entryRepo.findByQueueIdAndUserIdAndStatus("q1", "u1",
                QueueEntry.EntryStatus.WAITING).isPresent());
        assertFalse(entryRepo.findByQueueIdAndUserIdAndStatus("q1", "u1",
                QueueEntry.EntryStatus.SERVED).isPresent());
    }

    // ── NotificationRepository ────────────────────────────────

    @Test
    void notification_FindUnreadByUserId_FiltersReadOnes() {
        notifRepo.save(Notification.builder()
                .id("n1").user(savedUser).message("Hello").read(false)
                .type(Notification.NotificationType.QUEUE_JOINED).build());
        notifRepo.save(Notification.builder()
                .id("n2").user(savedUser).message("Done").read(true)
                .type(Notification.NotificationType.YOUR_TURN).build());

        List<Notification> unread = notifRepo.findByUserIdAndReadFalseOrderByCreatedAtDesc("u1");
        assertEquals(1, unread.size());
        assertEquals("n1", unread.get(0).getId());
    }

    @Test
    void notification_CountUnread_IsCorrect() {
        notifRepo.save(Notification.builder()
                .id("n1").user(savedUser).message("A").read(false)
                .type(Notification.NotificationType.QUEUE_JOINED).build());
        notifRepo.save(Notification.builder()
                .id("n2").user(savedUser).message("B").read(false)
                .type(Notification.NotificationType.ALMOST_YOUR_TURN).build());
        notifRepo.save(Notification.builder()
                .id("n3").user(savedUser).message("C").read(true)
                .type(Notification.NotificationType.YOUR_TURN).build());

        assertEquals(2, notifRepo.countByUserIdAndReadFalse("u1"));
    }

    // ── HistoryRecordRepository ───────────────────────────────

    @Test
    void history_FindByUserId_ReturnsMostRecentFirst() {
        historyRepo.save(HistoryRecord.builder()
                .id("h1").user(savedUser).serviceName("Advising")
                .joinedAt(LocalDateTime.now().minusHours(2))
                .finalStatus(QueueEntry.EntryStatus.SERVED).waitedMinutes(10).build());
        historyRepo.save(HistoryRecord.builder()
                .id("h2").user(savedUser).serviceName("Clinic")
                .joinedAt(LocalDateTime.now().minusHours(1))
                .finalStatus(QueueEntry.EntryStatus.LEFT).waitedMinutes(5).build());

        List<HistoryRecord> records = historyRepo.findByUserIdOrderByJoinedAtDesc("u1");
        assertEquals(2, records.size());
        assertEquals("h2", records.get(0).getId()); // most recent first
    }

    @Test
    void history_CountByFinalStatus_IsCorrect() {
        historyRepo.save(HistoryRecord.builder()
                .id("h1").user(savedUser).serviceName("A")
                .joinedAt(LocalDateTime.now())
                .finalStatus(QueueEntry.EntryStatus.SERVED).waitedMinutes(5).build());
        historyRepo.save(HistoryRecord.builder()
                .id("h2").user(savedUser).serviceName("B")
                .joinedAt(LocalDateTime.now())
                .finalStatus(QueueEntry.EntryStatus.LEFT).waitedMinutes(3).build());

        assertEquals(1, historyRepo.countByFinalStatus(QueueEntry.EntryStatus.SERVED));
        assertEquals(1, historyRepo.countByFinalStatus(QueueEntry.EntryStatus.LEFT));
    }

    @Test
    void history_GroupedStats_ReturnCorrectCounts() {
        historyRepo.save(HistoryRecord.builder().id("h1").user(savedUser)
                .serviceName("Advising").joinedAt(LocalDateTime.now())
                .finalStatus(QueueEntry.EntryStatus.SERVED).waitedMinutes(10).build());
        historyRepo.save(HistoryRecord.builder().id("h2").user(savedUser)
                .serviceName("Advising").joinedAt(LocalDateTime.now())
                .finalStatus(QueueEntry.EntryStatus.SERVED).waitedMinutes(20).build());
        historyRepo.save(HistoryRecord.builder().id("h3").user(savedUser)
                .serviceName("Clinic").joinedAt(LocalDateTime.now())
                .finalStatus(QueueEntry.EntryStatus.LEFT).waitedMinutes(5).build());

        List<Object[]> counts = historyRepo.countGroupedByServiceName();
        assertEquals(2, counts.size()); // 2 distinct service names

        List<Object[]> avgs = historyRepo.avgWaitGroupedByServiceName();
        assertEquals(2, avgs.size());
    }
}




