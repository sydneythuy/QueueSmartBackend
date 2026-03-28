package com.queuesmart.service;

import com.queuesmart.dto.ServiceDto;
import com.queuesmart.model.Service;
import com.queuesmart.repository.QueueRepository;
import com.queuesmart.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
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
class ServiceManagementServiceTest {

    @Mock private ServiceRepository serviceRepository;
    @Mock private QueueRepository queueRepository;

    @InjectMocks private ServiceManagementService serviceManagementService;

    private ServiceDto.CreateServiceRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new ServiceDto.CreateServiceRequest();
        createRequest.setName("Advising");
        createRequest.setDescription("Academic advising service");
        createRequest.setExpectedDurationMinutes(15);
        createRequest.setPriorityLevel(Service.PriorityLevel.MEDIUM);
    }

    // ---- CREATE ----

    @Test
    void createService_Success_ReturnsServiceResponse() {
        when(serviceRepository.existsByName("Advising")).thenReturn(false);
        when(serviceRepository.save(any(Service.class))).thenAnswer(i -> i.getArgument(0));
        when(queueRepository.countActiveByServiceId(anyString())).thenReturn(0);

        ServiceDto.ServiceResponse response = serviceManagementService.createService(createRequest, "admin-1");

        assertNotNull(response);
        assertEquals("Advising", response.getName());
        assertEquals(15, response.getExpectedDurationMinutes());
        assertTrue(response.isActive());
    }

    @Test
    void createService_DuplicateName_ThrowsException() {
        when(serviceRepository.existsByName("Advising")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> serviceManagementService.createService(createRequest, "admin-1"));
        verify(serviceRepository, never()).save(any());
    }

    // ---- UPDATE ----

    @Test
    void updateService_Success_UpdatesFields() {
        Service existing = Service.builder()
                .id("svc-1").name("Advising").description("Old desc")
                .expectedDurationMinutes(15).priorityLevel(Service.PriorityLevel.MEDIUM)
                .active(true).build();

        ServiceDto.UpdateServiceRequest updateRequest = new ServiceDto.UpdateServiceRequest();
        updateRequest.setDescription("New description");
        updateRequest.setExpectedDurationMinutes(20);

        when(serviceRepository.findById("svc-1")).thenReturn(Optional.of(existing));
        when(serviceRepository.save(any(Service.class))).thenAnswer(i -> i.getArgument(0));
        when(queueRepository.countActiveByServiceId("svc-1")).thenReturn(3);

        ServiceDto.ServiceResponse response = serviceManagementService.updateService("svc-1", updateRequest, "admin-1");

        assertEquals("New description", response.getDescription());
        assertEquals(20, response.getExpectedDurationMinutes());
        assertEquals(3, response.getCurrentQueueSize());
    }

    @Test
    void updateService_NotFound_ThrowsException() {
        when(serviceRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> serviceManagementService.updateService("bad-id", new ServiceDto.UpdateServiceRequest(), "admin-1"));
    }

    // ---- GET ALL ----

    @Test
    void getAllServices_ReturnsList() {
        Service s1 = Service.builder().id("s1").name("S1").active(true).build();
        Service s2 = Service.builder().id("s2").name("S2").active(false).build();
        when(serviceRepository.findAll()).thenReturn(List.of(s1, s2));
        when(queueRepository.countActiveByServiceId(anyString())).thenReturn(0);

        List<ServiceDto.ServiceResponse> all = serviceManagementService.getAllServices();
        assertEquals(2, all.size());
    }

    @Test
    void getActiveServices_ReturnsOnlyActive() {
        Service s1 = Service.builder().id("s1").name("S1").active(true).build();
        when(serviceRepository.findAllActive()).thenReturn(List.of(s1));
        when(queueRepository.countActiveByServiceId(anyString())).thenReturn(2);

        List<ServiceDto.ServiceResponse> active = serviceManagementService.getActiveServices();
        assertEquals(1, active.size());
    }

    // ---- DELETE ----

    @Test
    void deleteService_Success() {
        when(serviceRepository.existsById("svc-1")).thenReturn(true);

        serviceManagementService.deleteService("svc-1");

        verify(serviceRepository).delete("svc-1");
    }

    @Test
    void deleteService_NotFound_ThrowsException() {
        when(serviceRepository.existsById("bad")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> serviceManagementService.deleteService("bad"));
    }
}
