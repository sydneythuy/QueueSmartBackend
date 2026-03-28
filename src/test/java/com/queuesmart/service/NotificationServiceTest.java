package com.queuesmart.service;

import com.queuesmart.model.Notification;
import com.queuesmart.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;

    @InjectMocks private NotificationService notificationService;

    @Test
    void sendQueueJoined_SavesNotificationWithCorrectType() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.sendQueueJoined("user-1", "Advising", 3);

        assertNotNull(result);
        assertEquals(Notification.NotificationType.QUEUE_JOINED, result.getType());
        assertEquals("user-1", result.getUserId());
        assertTrue(result.getMessage().contains("Advising"));
        assertTrue(result.getMessage().contains("#3"));
        assertFalse(result.isRead());
    }

    @Test
    void sendAlmostYourTurn_ContainsPositionInfo() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.sendAlmostYourTurn("user-1", "IT Support", 2);

        assertEquals(Notification.NotificationType.ALMOST_YOUR_TURN, result.getType());
        assertTrue(result.getMessage().contains("#2"));
    }

    @Test
    void sendYourTurn_SetsCorrectType() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.sendYourTurn("user-1", "Clinic");

        assertEquals(Notification.NotificationType.YOUR_TURN, result.getType());
        assertTrue(result.getMessage().contains("Clinic"));
    }

    @Test
    void sendQueueLeft_SetsCorrectType() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.sendQueueLeft("user-1", "Advising");

        assertEquals(Notification.NotificationType.QUEUE_LEFT, result.getType());
    }

    @Test
    void getNotificationsForUser_DelegatesToRepository() {
        Notification n1 = Notification.builder().id("n1").userId("user-1").build();
        when(notificationRepository.findByUserId("user-1")).thenReturn(List.of(n1));

        List<Notification> results = notificationService.getNotificationsForUser("user-1");

        assertEquals(1, results.size());
    }

    @Test
    void getUnreadCount_ReturnsCorrectCount() {
        when(notificationRepository.countUnread("user-1")).thenReturn(5);

        assertEquals(5, notificationService.getUnreadCount("user-1"));
    }

    @Test
    void markAsRead_CallsRepository() {
        notificationService.markAsRead("notif-1");
        verify(notificationRepository).markAsRead("notif-1");
    }


    @Test
    void testMarkAllAsRead_marksAllNotificationsRead() {
        notificationService.sendQueueJoined("user1", "Advising", 2);
        notificationService.sendQueueJoined("user1", "Advising", 1);
        notificationService.markAllAsRead("user1");
        assertEquals(0, notificationService.getUnreadCount("user1"));
    }

    @Test
    void testGetNotificationsByType_filtersCorrectly() {
        notificationService.sendQueueJoined("user2", "Health", 1);
        notificationService.sendQueueLeft("user2", "Health");
        var joined = notificationService.getNotificationsByType("user2", "joined");
        assertFalse(joined.isEmpty());
        joined.forEach(n -> assertTrue(n.getMessage().toLowerCase().contains("joined")));
    }

    @Test
    void testGetTotalNotificationCount_returnsCorrectCount() {
        notificationService.sendQueueJoined("user3", "IT Support", 3);
        notificationService.sendAlmostYourTurn("user3", "IT Support", 2);
        notificationService.sendYourTurn("user3", "IT Support");
        assertEquals(3, notificationService.getTotalNotificationCount("user3"));
    }

}
