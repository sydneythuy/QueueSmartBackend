package com.queuesmart.service;

import com.queuesmart.model.*;
import com.queuesmart.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for in-memory repository implementations.
 * These validate the storage and retrieval logic used in place of a real DB.
 */
class RepositoryTest {

    // ---- UserRepository ----

    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
    }

    @Test
    void userRepository_SaveAndFindById() {
        User user = User.builder().id("u1").username("alice").email("a@b.com")
                .role(User.Role.USER).build();
        userRepository.save(user);
        Optional<User> found = userRepository.findById("u1");
        assertTrue(found.isPresent());
        assertEquals("alice", found.get().getUsername());
    }

    @Test
    void userRepository_FindByEmail_ReturnsCorrectUser() {
        User user = User.builder().id("u1").username("alice").email("alice@x.com")
                .role(User.Role.USER).build();
        userRepository.save(user);
        assertTrue(userRepository.findByEmail("alice@x.com").isPresent());
        assertFalse(userRepository.findByEmail("other@x.com").isPresent());
    }

    @Test
    void userRepository_ExistsByEmail_ReturnsTrueWhenPresent() {
        User user = User.builder().id("u1").email("a@b.com").username("a").role(User.Role.USER).build();
        userRepository.save(user);
        assertTrue(userRepository.existsByEmail("a@b.com"));
        assertFalse(userRepository.existsByEmail("nope@b.com"));
    }

    @Test
    void userRepository_ExistsByUsername_CaseInsensitive() {
        User user = User.builder().id("u1").email("a@b.com").username("Alice").role(User.Role.USER).build();
        userRepository.save(user);
        assertTrue(userRepository.existsByUsername("alice"));
        assertTrue(userRepository.existsByUsername("ALICE"));
    }

    @Test
    void userRepository_Delete_RemovesUser() {
        User user = User.builder().id("u1").email("a@b.com").username("alice").role(User.Role.USER).build();
        userRepository.save(user);
        userRepository.delete("u1");
        assertFalse(userRepository.findById("u1").isPresent());
        assertFalse(userRepository.existsByEmail("a@b.com"));
    }

    @Test
    void userRepository_Count_ReturnsCorrectNumber() {
        assertEquals(0, userRepository.count());
        userRepository.save(User.builder().id("u1").email("a@b.com").username("a").role(User.Role.USER).build());
        userRepository.save(User.builder().id("u2").email("b@b.com").username("b").role(User.Role.USER).build());
        assertEquals(2, userRepository.count());
    }

    // ---- ServiceRepository ----

    @Test
    void serviceRepository_SaveAndRetrieve() {
        ServiceRepository repo = new ServiceRepository();
        Service svc = Service.builder().id("s1").name("Advising").active(true).build();
        repo.save(svc);
        assertTrue(repo.findById("s1").isPresent());
        assertTrue(repo.existsById("s1"));
        assertTrue(repo.existsByName("advising")); // case-insensitive
    }

    @Test
    void serviceRepository_FindAllActive_FiltersCorrectly() {
        ServiceRepository repo = new ServiceRepository();
        repo.save(Service.builder().id("s1").name("A").active(true).build());
        repo.save(Service.builder().id("s2").name("B").active(false).build());
        List<Service> active = repo.findAllActive();
        assertEquals(1, active.size());
        assertEquals("s1", active.get(0).getId());
    }

    @Test
    void serviceRepository_Delete_RemovesService() {
        ServiceRepository repo = new ServiceRepository();
        repo.save(Service.builder().id("s1").name("A").active(true).build());
        repo.delete("s1");
        assertFalse(repo.existsById("s1"));
    }

    // ---- QueueRepository ----

    @Test
    void queueRepository_FindActiveByServiceId_ReturnsOnlyWaiting() {
        QueueRepository repo = new QueueRepository();
        QueueEntry waiting = QueueEntry.builder().id("e1").userId("u1").serviceId("svc-1")
                .joinedAt(LocalDateTime.now()).status(QueueEntry.QueueStatus.WAITING)
                .priorityLevel(Service.PriorityLevel.MEDIUM).build();
        QueueEntry served = QueueEntry.builder().id("e2").userId("u2").serviceId("svc-1")
                .joinedAt(LocalDateTime.now()).status(QueueEntry.QueueStatus.SERVED)
                .priorityLevel(Service.PriorityLevel.MEDIUM).build();
        repo.save(waiting);
        repo.save(served);

        List<QueueEntry> active = repo.findActiveByServiceId("svc-1");
        assertEquals(1, active.size());
        assertEquals("e1", active.get(0).getId());
    }

    @Test
    void queueRepository_PriorityOrdering_HighBeforeLow() {
        QueueRepository repo = new QueueRepository();
        LocalDateTime now = LocalDateTime.now();

        QueueEntry low = QueueEntry.builder().id("e1").userId("u1").serviceId("svc-1")
                .joinedAt(now).status(QueueEntry.QueueStatus.WAITING)
                .priorityLevel(Service.PriorityLevel.LOW).build();
        QueueEntry high = QueueEntry.builder().id("e2").userId("u2").serviceId("svc-1")
                .joinedAt(now.plusSeconds(1)).status(QueueEntry.QueueStatus.WAITING)
                .priorityLevel(Service.PriorityLevel.HIGH).build();
        repo.save(low);
        repo.save(high);

        List<QueueEntry> sorted = repo.findActiveByServiceId("svc-1");
        assertEquals("e2", sorted.get(0).getId()); // HIGH priority first
        assertEquals("e1", sorted.get(1).getId());
    }

    @Test
    void queueRepository_FindByUserIdAndServiceId_ReturnsWaitingEntry() {
        QueueRepository repo = new QueueRepository();
        QueueEntry entry = QueueEntry.builder().id("e1").userId("u1").serviceId("svc-1")
                .joinedAt(LocalDateTime.now()).status(QueueEntry.QueueStatus.WAITING)
                .priorityLevel(Service.PriorityLevel.MEDIUM).build();
        repo.save(entry);

        assertTrue(repo.findByUserIdAndServiceId("u1", "svc-1").isPresent());
        assertFalse(repo.findByUserIdAndServiceId("u2", "svc-1").isPresent());
    }

    @Test
    void queueRepository_CountActiveByServiceId_IsCorrect() {
        QueueRepository repo = new QueueRepository();
        repo.save(QueueEntry.builder().id("e1").userId("u1").serviceId("svc-1")
                .joinedAt(LocalDateTime.now()).status(QueueEntry.QueueStatus.WAITING)
                .priorityLevel(Service.PriorityLevel.LOW).build());
        repo.save(QueueEntry.builder().id("e2").userId("u2").serviceId("svc-1")
                .joinedAt(LocalDateTime.now()).status(QueueEntry.QueueStatus.SERVED)
                .priorityLevel(Service.PriorityLevel.LOW).build());

        assertEquals(1, repo.countActiveByServiceId("svc-1"));
    }

    // ---- NotificationRepository ----

    @Test
    void notificationRepository_FindUnread_FiltersRead() {
        NotificationRepository repo = new NotificationRepository();
        Notification unread = Notification.builder().id("n1").userId("u1")
                .createdAt(LocalDateTime.now()).read(false).build();
        Notification read = Notification.builder().id("n2").userId("u1")
                .createdAt(LocalDateTime.now()).read(true).build();
        repo.save(unread);
        repo.save(read);

        List<Notification> unreadList = repo.findUnreadByUserId("u1");
        assertEquals(1, unreadList.size());
        assertEquals("n1", unreadList.get(0).getId());
    }

    @Test
    void notificationRepository_MarkAsRead_UpdatesFlag() {
        NotificationRepository repo = new NotificationRepository();
        Notification n = Notification.builder().id("n1").userId("u1")
                .createdAt(LocalDateTime.now()).read(false).build();
        repo.save(n);
        repo.markAsRead("n1");
        assertTrue(repo.findById("n1").get().isRead());
    }

    @Test
    void notificationRepository_CountUnread_ReturnsCorrect() {
        NotificationRepository repo = new NotificationRepository();
        repo.save(Notification.builder().id("n1").userId("u1").createdAt(LocalDateTime.now()).read(false).build());
        repo.save(Notification.builder().id("n2").userId("u1").createdAt(LocalDateTime.now()).read(false).build());
        repo.save(Notification.builder().id("n3").userId("u1").createdAt(LocalDateTime.now()).read(true).build());
        assertEquals(2, repo.countUnread("u1"));
    }

    // ---- HistoryRepository ----

    @Test
    void historyRepository_FindByUserId_ReturnsCorrect() {
        HistoryRepository repo = new HistoryRepository();
        HistoryRecord r1 = HistoryRecord.builder().id("h1").userId("u1").serviceId("s1")
                .joinedAt(LocalDateTime.now()).finalStatus(QueueEntry.QueueStatus.SERVED).build();
        HistoryRecord r2 = HistoryRecord.builder().id("h2").userId("u2").serviceId("s1")
                .joinedAt(LocalDateTime.now()).finalStatus(QueueEntry.QueueStatus.LEFT).build();
        repo.save(r1);
        repo.save(r2);

        List<HistoryRecord> userHistory = repo.findByUserId("u1");
        assertEquals(1, userHistory.size());
        assertEquals("h1", userHistory.get(0).getId());
    }

    @Test
    void historyRepository_CountByServiceId_IsCorrect() {
        HistoryRepository repo = new HistoryRepository();
        repo.save(HistoryRecord.builder().id("h1").userId("u1").serviceId("svc-1")
                .joinedAt(LocalDateTime.now()).finalStatus(QueueEntry.QueueStatus.SERVED).build());
        repo.save(HistoryRecord.builder().id("h2").userId("u2").serviceId("svc-1")
                .joinedAt(LocalDateTime.now()).finalStatus(QueueEntry.QueueStatus.SERVED).build());
        repo.save(HistoryRecord.builder().id("h3").userId("u3").serviceId("svc-2")
                .joinedAt(LocalDateTime.now()).finalStatus(QueueEntry.QueueStatus.SERVED).build());

        assertEquals(2, repo.countByServiceId("svc-1"));
        assertEquals(1, repo.countByServiceId("svc-2"));
    }
}
