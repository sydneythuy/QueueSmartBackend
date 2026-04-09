package com.queuesmart.controller;

import com.queuesmart.dto.ApiResponse;
import com.queuesmart.model.Notification;
import com.queuesmart.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /api/notifications
     * All notifications for the logged-in user — matches the Notifications screen
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved",
                notificationService.getNotificationsForUser(userId)));
    }

    /**
     * GET /api/notifications/unread
     * Unread notifications — used for the notification badge count
     */
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<Notification>>> getUnreadNotifications(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Unread notifications",
                notificationService.getUnreadNotificationsForUser(userId)));
    }

    /**
     * GET /api/notifications/count
     * Returns unread count — for the badge in the UI header
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getUnreadCount(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        int count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success("Unread count", Map.of("unreadCount", count)));
    }

    /**
     * PATCH /api/notifications/{id}/read
     * Marks a specific notification as read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }
}
