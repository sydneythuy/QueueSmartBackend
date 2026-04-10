package com.queuesmart.service;

import com.queuesmart.dto.ServiceDto;
import com.queuesmart.model.Queue;
import com.queuesmart.model.Service;
import com.queuesmart.repository.QueueEntryRepository;
import com.queuesmart.repository.QueueRepository;
import com.queuesmart.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceManagementService {

    private final ServiceRepository    serviceRepository;
    private final QueueRepository      queueRepository;
    private final QueueEntryRepository queueEntryRepository;

    @Transactional
    public ServiceDto.ServiceResponse createService(ServiceDto.CreateServiceRequest req, String adminId) {
        if (serviceRepository.existsByNameIgnoreCase(req.getName())) {
            throw new IllegalArgumentException("A service with that name already exists");
        }

        Service service = Service.builder()
                .id(UUID.randomUUID().toString())
                .name(req.getName())
                .description(req.getDescription())
                .expectedDurationMinutes(req.getExpectedDurationMinutes())
                .priorityLevel(req.getPriorityLevel())
                .createdByAdminId(adminId)
                .active(true)
                .build();
        serviceRepository.save(service);

        // Create a corresponding open Queue row immediately
        Queue queue = Queue.builder()
                .id(UUID.randomUUID().toString())
                .service(service)
                .status(Queue.QueueStatus.OPEN)
                .build();
        queueRepository.save(queue);

        return toResponse(service);
    }

    @Transactional
    public ServiceDto.ServiceResponse updateService(String id, ServiceDto.UpdateServiceRequest req, String adminId) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        if (req.getName() != null && !req.getName().equalsIgnoreCase(service.getName())) {
            if (serviceRepository.existsByNameIgnoreCase(req.getName())) {
                throw new IllegalArgumentException("A service with that name already exists");
            }
            service.setName(req.getName());
        }
        if (req.getDescription() != null)           service.setDescription(req.getDescription());
        if (req.getExpectedDurationMinutes() != null)
            service.setExpectedDurationMinutes(req.getExpectedDurationMinutes());
        if (req.getPriorityLevel() != null)          service.setPriorityLevel(req.getPriorityLevel());
        if (req.getActive() != null)                 service.setActive(req.getActive());

        serviceRepository.save(service);
        return toResponse(service);
    }

    @Transactional(readOnly = true)
    public List<ServiceDto.ServiceResponse> getAllServices() {
        return serviceRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceDto.ServiceResponse> getActiveServices() {
        return serviceRepository.findAllByActiveTrue().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceDto.ServiceResponse getServiceById(String id) {
        return toResponse(serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service not found")));
    }

    @Transactional
    public void deleteService(String id) {
        if (!serviceRepository.existsById(id)) throw new IllegalArgumentException("Service not found");
        serviceRepository.deleteById(id);
    }

    // ── internal helper used by QueueService ──────────────────
    @Transactional(readOnly = true)
    public Service getRawService(String serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
    }

    // ── private ───────────────────────────────────────────────
    private ServiceDto.ServiceResponse toResponse(Service s) {
        int queueSize = queueRepository.findByServiceId(s.getId())
                .map(q -> (int) queueEntryRepository.countByQueueIdAndStatus(
                        q.getId(), com.queuesmart.model.QueueEntry.EntryStatus.WAITING))
                .orElse(0);
        return new ServiceDto.ServiceResponse(s, queueSize);
    }
}



