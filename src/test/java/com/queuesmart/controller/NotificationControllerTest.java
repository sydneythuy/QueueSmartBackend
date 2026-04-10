package com.queuesmart.controller;

import com.queuesmart.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;
    @Mock private NotificationService notificationService;
    @InjectMocks private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
    }

    private UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken("u1", null, List.of());
    }

    @Test
    void getMyNotifications_ReturnsOk() throws Exception {
        when(notificationService.getNotificationsForUser(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/notifications").principal(userAuth()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getUnreadNotifications_ReturnsOk() throws Exception {
        when(notificationService.getUnreadNotificationsForUser(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/notifications/unread").principal(userAuth()))
                .andExpect(status().isOk());
    }

    @Test
    void getUnreadCount_ReturnsCount() throws Exception {
        when(notificationService.getUnreadCount(any())).thenReturn(3);
        mockMvc.perform(get("/api/notifications/count").principal(userAuth()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    @Test
    void markAsRead_ReturnsOk() throws Exception {
        doNothing().when(notificationService).markAsRead("n1");
        mockMvc.perform(patch("/api/notifications/n1/read"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }
}
