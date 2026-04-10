package com.queuesmart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queuesmart.dto.ServiceDto;
import com.queuesmart.model.Service;
import com.queuesmart.service.ServiceManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class ServiceControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private ServiceManagementService serviceManagementService;
    @InjectMocks private ServiceController serviceController;

    private ServiceDto.ServiceResponse cachedResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(serviceController).build();

        // Build the mock response ONCE here, outside of any when() calls
        Service svc = Mockito.mock(Service.class);
        lenient().when(svc.getId()).thenReturn("svc-1");
        lenient().when(svc.getName()).thenReturn("Advising");
        lenient().when(svc.getDescription()).thenReturn("Academic advising");
        lenient().when(svc.getExpectedDurationMinutes()).thenReturn(15);
        lenient().when(svc.getPriorityLevel()).thenReturn(Service.PriorityLevel.MEDIUM);
        lenient().when(svc.isActive()).thenReturn(true);
        cachedResponse = new ServiceDto.ServiceResponse(svc, 0);
    }

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken("admin-1", null, List.of());
    }

    @Test
    void getServices_ReturnsActiveServices() throws Exception {
        when(serviceManagementService.getActiveServices()).thenReturn(List.of(cachedResponse));
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Advising"));
    }

    @Test
    void getServiceById_ReturnsService() throws Exception {
        when(serviceManagementService.getServiceById("svc-1")).thenReturn(cachedResponse);
        mockMvc.perform(get("/api/services/svc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("svc-1"));
    }

    @Test
    void createService_ValidRequest_Returns201() throws Exception {
        ServiceDto.CreateServiceRequest req = new ServiceDto.CreateServiceRequest();
        req.setName("Advising"); req.setDescription("Academic advising sessions");
        req.setExpectedDurationMinutes(15); req.setPriorityLevel(Service.PriorityLevel.MEDIUM);
        when(serviceManagementService.createService(any(), any())).thenReturn(cachedResponse);
        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)).principal(adminAuth()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.name").value("Advising"));
    }

    @Test
    void deleteService_ReturnsOk() throws Exception {
        doNothing().when(serviceManagementService).deleteService("svc-1");
        mockMvc.perform(delete("/api/services/svc-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAllServices_ReturnsAll() throws Exception {
        when(serviceManagementService.getAllServices()).thenReturn(List.of(cachedResponse));
        mockMvc.perform(get("/api/services/all"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value("svc-1"));
    }
}
