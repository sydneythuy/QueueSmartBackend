package com.queuesmart.service;

import com.queuesmart.dto.ServiceDto;
import com.queuesmart.model.Service;
import com.queuesmart.repository.QueueRepository;
import com.queuesmart.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final QueueRepository queueRepository;

    public ServiceDto.ServiceResponse createService(ServiceDto.CreateServiceRequest request, String adminId) {
        if (serviceRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("A service with that name already exists");
        }

        Service service = Service.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .description(request.getDescription())
                .expectedDurationMinutes(request.getExpectedDurationMinutes())
                .priorityLevel(request.getPriorityLevel())
                .createdByAdminId(adminId)
                .active(true)
                .build();

        serviceRepository.save(service);
        int queueSize = queueRepository.countActiveByServiceId(service.getId());
        return new ServiceDto.ServiceResponse(service, queueSize);
    }

    public ServiceDto.ServiceResponse updateService(String serviceId,
                                                     ServiceDto.UpdateServiceRequest request,
                                                     String adminId) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        if (request.getName() != null) {
            boolean nameTaken = serviceRepository.existsByName(request.getName())
                    && !service.getName().equalsIgnoreCase(request.getName());
            if (nameTaken) throw new IllegalArgumentException("A service with that name already exists");
            service.setName(request.getName());
        }
        if (request.getDescription() != null)         service.setDescription(request.getDescription());
        if (request.getExpectedDurationMinutes() != null)
            service.setExpectedDurationMinutes(request.getExpectedDurationMinutes());
        if (request.getPriorityLevel() != null)        service.setPriorityLevel(request.getPriorityLevel());
        if (request.getActive() != null)               service.setActive(request.getActive());

        serviceRepository.save(service);
        int queueSize = queueRepository.countActiveByServiceId(service.getId());
        return new ServiceDto.ServiceResponse(service, queueSize);
    }

    public List<ServiceDto.ServiceResponse> getAllServices() {
        return serviceRepository.findAll().stream()
                .map(s -> new ServiceDto.ServiceResponse(s, queueRepository.countActiveByServiceId(s.getId())))
                .collect(Collectors.toList());
    }

    public List<ServiceDto.ServiceResponse> getActiveServices() {
        return serviceRepository.findAllActive().stream()
                .map(s -> new ServiceDto.ServiceResponse(s, queueRepository.countActiveByServiceId(s.getId())))
                .collect(Collectors.toList());
    }

    public ServiceDto.ServiceResponse getServiceById(String serviceId) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        int queueSize = queueRepository.countActiveByServiceId(serviceId);
        return new ServiceDto.ServiceResponse(service, queueSize);
    }

    public void deleteService(String serviceId) {
        if (!serviceRepository.existsById(serviceId)) {
            throw new IllegalArgumentException("Service not found");
        }
        serviceRepository.delete(serviceId);
    }

    // Internal helper for QueueService
    public Service getRawService(String serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
    }
}
