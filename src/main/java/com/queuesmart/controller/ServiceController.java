package com.queuesmart.controller;

import com.queuesmart.dto.ApiResponse;
import com.queuesmart.dto.ServiceDto;
import com.queuesmart.service.ServiceManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    /**
     * GET /api/services
     * Returns all active services — used by the frontend Join Queue screen
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceDto.ServiceResponse>>> getServices() {
        return ResponseEntity.ok(ApiResponse.success("Services retrieved",
                serviceManagementService.getActiveServices()));
    }

    /**
     * GET /api/services/all  (admin only)
     * Returns all services including inactive ones
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ServiceDto.ServiceResponse>>> getAllServices() {
        return ResponseEntity.ok(ApiResponse.success("All services retrieved",
                serviceManagementService.getAllServices()));
    }

    /**
     * GET /api/services/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceDto.ServiceResponse>> getServiceById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Service retrieved",
                serviceManagementService.getServiceById(id)));
    }

    /**
     * POST /api/services  (admin only)
     * Body: { name, description, expectedDurationMinutes, priorityLevel }
     * Matches the "New service" form in the frontend
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceDto.ServiceResponse>> createService(
            @Valid @RequestBody ServiceDto.CreateServiceRequest request,
            Authentication auth) {

        String adminId = (String) auth.getPrincipal();
        ServiceDto.ServiceResponse response = serviceManagementService.createService(request, adminId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service created successfully", response));
    }

    /**
     * PUT /api/services/{id}  (admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceDto.ServiceResponse>> updateService(
            @PathVariable String id,
            @Valid @RequestBody ServiceDto.UpdateServiceRequest request,
            Authentication auth) {

        String adminId = (String) auth.getPrincipal();
        ServiceDto.ServiceResponse response = serviceManagementService.updateService(id, request, adminId);
        return ResponseEntity.ok(ApiResponse.success("Service updated successfully", response));
    }

    /**
     * DELETE /api/services/{id}  (admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable String id) {
        serviceManagementService.deleteService(id);
        return ResponseEntity.ok(ApiResponse.success("Service deleted", null));
    }
}
