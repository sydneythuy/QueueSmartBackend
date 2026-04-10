package com.queuesmart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queuesmart.dto.QueueDto;
import com.queuesmart.model.QueueEntry;
import com.queuesmart.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QueueControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private QueueService queueService;
    @InjectMocks private QueueController queueController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(queueController).build();
    }

    private UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken("u1", null, List.of());
    }

    private QueueDto.QueueEntryResponse mockEntry() {
        QueueDto.QueueEntryResponse r = new QueueDto.QueueEntryResponse();
        r.setId("e1"); r.setUserId("u1"); r.setPosition(1);
        r.setStatus(QueueEntry.EntryStatus.WAITING);
        return r;
    }

    @Test
    void getQueueStatus_ReturnsOk() throws Exception {
        QueueDto.QueueStatusResponse status = new QueueDto.QueueStatusResponse();
        status.setServiceId("svc-1"); status.setTotalWaiting(2); status.setEntries(List.of());
        when(queueService.getQueueStatus("svc-1")).thenReturn(status);
        mockMvc.perform(get("/api/queue/status/svc-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalWaiting").value(2));
    }

    @Test
    void joinQueue_ValidRequest_Returns201() throws Exception {
        QueueDto.JoinQueueRequest req = new QueueDto.JoinQueueRequest();
        req.setServiceId("svc-1");
        when(queueService.joinQueue(any(), any(), any())).thenReturn(mockEntry());
        mockMvc.perform(post("/api/queue/join").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)).principal(userAuth()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.position").value(1));
    }

    @Test
    void leaveQueue_ReturnsOk() throws Exception {
        doNothing().when(queueService).leaveQueue(any(), any());
        mockMvc.perform(delete("/api/queue/leave/svc-1").principal(userAuth()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMyQueueEntry_ReturnsOk() throws Exception {
        when(queueService.getUserQueueEntry(any(), any())).thenReturn(mockEntry());
        mockMvc.perform(get("/api/queue/my/svc-1").principal(userAuth()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value("e1"));
    }

    @Test
    void serveNext_ReturnsOk() throws Exception {
        when(queueService.serveNext("svc-1")).thenReturn(mockEntry());
        mockMvc.perform(post("/api/queue/serve/svc-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }
}
