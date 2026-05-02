package com.queuesmart.controller;

import com.queuesmart.dto.ApiResponse;
import com.queuesmart.dto.QueueDto;
import com.queuesmart.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    /**
     * POST /api/queue/join
     * Body: { serviceId, priorityLevel? }
     */
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<QueueDto.QueueEntryResponse>> joinQueue(
            @Valid @RequestBody QueueDto.JoinQueueRequest request,
            Authentication auth) {

        String userId = (String) auth.getPrincipal();
        QueueDto.QueueEntryResponse response = queueService.joinQueue(
                userId, request.getServiceId(), request.getPriorityLevel());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Joined queue successfully", response));
    }

    /**
     * DELETE /api/queue/leave/{serviceId}
     */
    @DeleteMapping("/leave/{serviceId}")
    public ResponseEntity<ApiResponse<Void>> leaveQueue(
            @PathVariable String serviceId,
            Authentication auth) {

        String userId = (String) auth.getPrincipal();
        queueService.leaveQueue(userId, serviceId);
        return ResponseEntity.ok(ApiResponse.success("Left queue successfully", null));
    }

    /**
     * GET /api/queue/status/{serviceId}
     */
    @GetMapping("/status/{serviceId}")
    public ResponseEntity<ApiResponse<QueueDto.QueueStatusResponse>> getQueueStatus(
            @PathVariable String serviceId) {

        return ResponseEntity.ok(ApiResponse.success("Queue status retrieved",
                queueService.getQueueStatus(serviceId)));
    }

    /**
     * GET /api/queue/my
     * Returns ALL active queue entries for the authenticated user across all services.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<QueueDto.QueueEntryResponse>>> getAllMyQueueEntries(
            Authentication auth) {

        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Your active queues",
                queueService.getAllUserQueueEntries(userId)));
    }

    /**
     * GET /api/queue/my/{serviceId}
     * Returns the authenticated user's queue entry for a specific service.
     */
    @GetMapping("/my/{serviceId}")
    public ResponseEntity<ApiResponse<QueueDto.QueueEntryResponse>> getMyQueueEntry(
            @PathVariable String serviceId,
            Authentication auth) {

        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Your queue entry",
                queueService.getUserQueueEntry(userId, serviceId)));
    }

    /**
     * POST /api/queue/serve/{serviceId}  (admin only)
     */
    @PostMapping("/serve/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<QueueDto.QueueEntryResponse>> serveNext(
            @PathVariable String serviceId) {

        QueueDto.QueueEntryResponse served = queueService.serveNext(serviceId);
        return ResponseEntity.ok(ApiResponse.success("User served successfully", served));
    }
}
