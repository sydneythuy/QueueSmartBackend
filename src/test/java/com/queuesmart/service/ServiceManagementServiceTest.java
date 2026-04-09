package com.queuesmart.service;

import com.queuesmart.dto.ServiceDto;
import com.queuesmart.model.Queue;
import com.queuesmart.model.QueueEntry;
import com.queuesmart.model.Service;
import com.queuesmart.repository.QueueEntryRepository;
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

    @Mock private ServiceRepository    serviceRepository;
    @Mock private QueueRepository      queueRepository;
    @Mock private QueueEntryRepository queueEntryRepository;

    @InjectMocks private ServiceManagementService serviceManagementService;

    private ServiceDto.CreateServiceRequest createReq;

    @BeforeEach
    void setUp() {
        createReq = new ServiceDto.CreateServiceRequest();
        createReq.setName("Advising");
        createReq.setDescription("Academic advising");
        createReq.setExpectedDurationMinutes(15);
        createReq.setPriorityLevel(Service.PriorityLevel.MEDIUM);
    }

    // ── CREATE ────────────────────────────────────────────────

    @Test
    void createService_Success_SavesServiceAndQueue() {
        when(serviceRepository.existsByNameIgnoreCase("Advising")).thenReturn(false);
        when(serviceRepository.save(any(Service.class))).thenAnswer(i -> i.getArgument(0));
        when(queueRepository.save(any(Queue.class))).thenAnswer(i -> i.getArgument(0));
        when(queueRepository.findByServiceId(anyString())).thenReturn(Optional.empty());

        ServiceDto.ServiceResponse resp = serviceManagementService.createService(createReq, "admin-1");

        assertEquals("Advising", resp.getName());
        assertTrue(resp.isActive());
        verify(serviceRepository).save(any(Service.class));
        verify(queueRepository).save(any(Queue.class));   // Queue row auto-created
    }

    @Test
    void createService_DuplicateName_ThrowsException() {
        when(serviceRepository.existsByNameIgnoreCase("Advising")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> serviceManagementService.createService(createReq, "admin-1"));
        verify(serviceRepository, never()).save(any());
    }

    // ── UPDATE ────────────────────────────────────────────────

    @Test
    void updateService_ChangeDuration_Persists() {
        Service existing = Service.builder().id("s1").name("Advising")
                .description("Old").expectedDurationMinutes(10)
                .priorityLevel(Service.PriorityLevel.LOW).active(true).build();

        ServiceDto.UpdateServiceRequest req = new ServiceDto.UpdateServiceRequest();
        req.setExpectedDurationMinutes(20);

        when(serviceRepository.findById("s1")).thenReturn(Optional.of(existing));
        when(serviceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(queueRepository.findByServiceId("s1")).thenReturn(Optional.empty());

        ServiceDto.ServiceResponse resp = serviceManagementService.updateService("s1", req, "admin-1");
        assertEquals(20, resp.getExpectedDurationMinutes());
    }

    @Test
    void updateService_NameConflict_ThrowsException() {
        Service existing = Service.builder().id("s1").name("Advising").build();
        ServiceDto.UpdateServiceRequest req = new ServiceDto.UpdateServiceRequest();
        req.setName("IT Support");

        when(serviceRepository.findById("s1")).thenReturn(Optional.of(existing));
        when(serviceRepository.existsByNameIgnoreCase("IT Support")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> serviceManagementService.updateService("s1", req, "admin-1"));
    }

    @Test
    void updateService_NotFound_ThrowsException() {
        when(serviceRepository.findById("bad")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> serviceManagementService.updateService("bad", new ServiceDto.UpdateServiceRequest(), "a"));
    }

    // ── GET ───────────────────────────────────────────────────

    @Test
    void getAllServices_ReturnsBothActiveAndInactive() {
        Service s1 = Service.builder().id("s1").name("A").active(true).build();
        Service s2 = Service.builder().id("s2").name("B").active(false).build();
        when(serviceRepository.findAll()).thenReturn(List.of(s1, s2));
        when(queueRepository.findByServiceId(anyString())).thenReturn(Optional.empty());

        assertEquals(2, serviceManagementService.getAllServices().size());
    }

    @Test
    void getActiveServices_ReturnsOnlyActive() {
        Service s1 = Service.builder().id("s1").name("A").active(true).build();
        when(serviceRepository.findAllByActiveTrue()).thenReturn(List.of(s1));
        when(queueRepository.findByServiceId("s1")).thenReturn(Optional.empty());

        assertEquals(1, serviceManagementService.getActiveServices().size());
    }

    @Test
    void getServiceById_Found_ReturnsResponse() {
        Service s = Service.builder().id("s1").name("Clinic").active(true)
                .priorityLevel(Service.PriorityLevel.HIGH).expectedDurationMinutes(25).build();
        when(serviceRepository.findById("s1")).thenReturn(Optional.of(s));
        when(queueRepository.findByServiceId("s1")).thenReturn(Optional.empty());

        ServiceDto.ServiceResponse resp = serviceManagementService.getServiceById("s1");
        assertEquals("Clinic", resp.getName());
    }

    @Test
    void getServiceById_NotFound_ThrowsException() {
        when(serviceRepository.findById("nope")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> serviceManagementService.getServiceById("nope"));
    }

    // ── DELETE ────────────────────────────────────────────────

    @Test
    void deleteService_Exists_CallsDeleteById() {
        when(serviceRepository.existsById("s1")).thenReturn(true);
        serviceManagementService.deleteService("s1");
        verify(serviceRepository).deleteById("s1");
    }

    @Test
    void deleteService_NotFound_ThrowsException() {
        when(serviceRepository.existsById("bad")).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> serviceManagementService.deleteService("bad"));
    }

    // ── QUEUE SIZE SHOWN IN RESPONSE ──────────────────────────

    @Test
    void createService_QueueSizeInResponse_ReflectsWaitingCount() {
        when(serviceRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(serviceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(queueRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Queue q = Queue.builder().id("q1").build();
        when(queueRepository.findByServiceId(anyString())).thenReturn(Optional.of(q));
        when(queueEntryRepository.countByQueueIdAndStatus("q1", QueueEntry.EntryStatus.WAITING))
                .thenReturn(4L);

        ServiceDto.ServiceResponse resp = serviceManagementService.createService(createReq, "admin-1");
        assertEquals(4, resp.getCurrentQueueSize());
    }
}
