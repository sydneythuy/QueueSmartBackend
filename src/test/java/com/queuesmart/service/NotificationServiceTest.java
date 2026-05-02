package com.queuesmart.service;

import com.queuesmart.model.Notification;
import com.queuesmart.model.UserCredential;
import com.queuesmart.repository.NotificationRepository;
import com.queuesmart.repository.UserCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository   notificationRepository;
    @Mock private UserCredentialRepository credentialRepository;

    @InjectMocks private NotificationService notificationService;

    private UserCredential mockUser() {
        return UserCredential.builder().id("u1").email("a@b.com")
                .role(UserCredential.Role.USER).build();
    }

    @Test
    void sendQueueJoined_SavesCorrectTypeAndMessage() {
        when(credentialRepository.findById("u1")).thenReturn(Optional.of(mockUser()));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification n = notificationService.sendQueueJoined("u1", "Advising", 3);

        assertEquals(Notification.NotificationType.QUEUE_JOINED, n.getType());
        assertTrue(n.getMessage().contains("Advising"));
        assertTrue(n.getMessage().contains("#3"));
        assertFalse(n.isRead());
    }

    @Test
    void sendAlmostYourTurn_ContainsPosition() {
        when(credentialRepository.findById("u1")).thenReturn(Optional.of(mockUser()));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification n = notificationService.sendAlmostYourTurn("u1", "IT Support", 2);

        assertEquals(Notification.NotificationType.ALMOST_YOUR_TURN, n.getType());
        assertTrue(n.getMessage().contains("#2"));
    }

    @Test
    void sendYourTurn_SetsCorrectType() {
        when(credentialRepository.findById("u1")).thenReturn(Optional.of(mockUser()));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification n = notificationService.sendYourTurn("u1", "Clinic");

        assertEquals(Notification.NotificationType.YOUR_TURN, n.getType());
        assertTrue(n.getMessage().contains("Clinic"));
    }

    @Test
    void sendQueueLeft_SetsCorrectType() {
        when(credentialRepository.findById("u1")).thenReturn(Optional.of(mockUser()));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification n = notificationService.sendQueueLeft("u1", "Advising");
        assertEquals(Notification.NotificationType.QUEUE_LEFT, n.getType());
    }

    @Test
    void sendNotification_UserNotFound_ThrowsException() {
        when(credentialRepository.findById("bad")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.sendQueueJoined("bad", "Clinic", 1));
    }

    @Test
    void getNotificationsForUser_DelegatesToRepo() {
        Notification n = Notification.builder().id("n1").build();
        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc("u1")).thenReturn(List.of(n));
        assertEquals(1, notificationService.getNotificationsForUser("u1").size());
    }

    @Test
    void getUnreadNotificationsForUser_DelegatesToRepo() {
        Notification n = Notification.builder().id("n1").read(false).build();
        when(notificationRepository.findByUser_IdAndReadFalseOrderByCreatedAtDesc("u1"))
                .thenReturn(List.of(n));
        assertEquals(1, notificationService.getUnreadNotificationsForUser("u1").size());
    }

    @Test
    void getUnreadCount_ReturnsCorrectValue() {
        when(notificationRepository.countByUser_IdAndReadFalse("u1")).thenReturn(7L);
        assertEquals(7, notificationService.getUnreadCount("u1"));
    }

    @Test
    void markAsRead_SetsReadFlagAndSaves() {
        Notification n = Notification.builder().id("n1").read(false).build();
        when(notificationRepository.findById("n1")).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.markAsRead("n1");

        assertTrue(n.isRead());
        verify(notificationRepository).save(n);
    }

    @Test
    void markAsRead_NotFound_DoesNotThrow() {
        when(notificationRepository.findById("bad")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> notificationService.markAsRead("bad"));
    }

    @Test
    void markAllAsRead_MarksAllUnreadAndSaves() {
        Notification n1 = Notification.builder().id("n1").read(false).build();
        Notification n2 = Notification.builder().id("n2").read(false).build();
        when(notificationRepository.findByUser_IdAndReadFalseOrderByCreatedAtDesc("u1"))
                .thenReturn(List.of(n1, n2));
        when(notificationRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.markAllAsRead("u1");

        assertTrue(n1.isRead());
        assertTrue(n2.isRead());
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void getNotificationsByType_FiltersCorrectly() {
        Notification n1 = Notification.builder().id("n1").message("joined Advising").build();
        Notification n2 = Notification.builder().id("n2").message("served at Clinic").build();
        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc("u1"))
                .thenReturn(List.of(n1, n2));

        List<Notification> filtered = notificationService.getNotificationsByType("u1", "joined");
        assertEquals(1, filtered.size());
        assertEquals("n1", filtered.get(0).getId());
    }
}
