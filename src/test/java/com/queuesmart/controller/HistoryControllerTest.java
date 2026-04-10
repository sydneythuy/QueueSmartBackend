package com.queuesmart.controller;

import com.queuesmart.service.HistoryService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HistoryControllerTest {

    private MockMvc mockMvc;
    @Mock private HistoryService historyService;
    @InjectMocks private HistoryController historyController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(historyController).build();
    }

    private UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken("u1", null, List.of());
    }

    @Test
    void getMyHistory_ReturnsOk() throws Exception {
        when(historyService.getUserHistory(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/history").principal(userAuth()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAllHistory_ReturnsOk() throws Exception {
        when(historyService.getAllHistory()).thenReturn(List.of());
        mockMvc.perform(get("/api/history/all"))
                .andExpect(status().isOk());
    }

    @Test
    void getStats_ReturnsOk() throws Exception {
        when(historyService.getUsageStatsByService()).thenReturn(Map.of());
        when(historyService.getAverageWaitByService()).thenReturn(Map.of());
        when(historyService.getTotalServed()).thenReturn(5L);
        mockMvc.perform(get("/api/history/stats"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }
}
