package com.queuesmart.controller;

import com.queuesmart.dto.ApiResponse;
import com.queuesmart.model.HistoryRecord;
import com.queuesmart.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    /**
     * GET /api/history
     * Returns current user's queue participation history — matches the History screen
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HistoryRecord>>> getMyHistory(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("History retrieved",
                historyService.getUserHistory(userId)));
    }

    /**
     * GET /api/history/all  (admin only)
     * All history records across all users
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<HistoryRecord>>> getAllHistory() {
        return ResponseEntity.ok(ApiResponse.success("All history retrieved",
                historyService.getAllHistory()));
    }

    /**
     * GET /api/history/stats  (admin only)
     * Returns usage statistics per service — matches the Admin Dashboard metrics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = Map.of(
                "usageByService",    historyService.getUsageStatsByService(),
                "avgWaitByService",  historyService.getAverageWaitByService(),
                "totalServed",       historyService.getTotalServed()
        );
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved", stats));
    }


    /**
     * GET /api/history/recent?limit=5
     * Returns the N most recent history entries for the authenticated user.
     */
    @org.springframework.web.bind.annotation.GetMapping("/recent")
    public org.springframework.http.ResponseEntity<com.queuesmart.dto.ApiResponse<java.util.List<com.queuesmart.model.HistoryRecord>>> getRecentHistory(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "5") int limit,
            org.springframework.security.core.Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return org.springframework.http.ResponseEntity.ok(
                com.queuesmart.dto.ApiResponse.success("Recent history",
                        historyService.getRecentHistory(userId, limit)));
    }

    /**
     * GET /api/history/count
     * Returns the total number of queue visits for the authenticated user.
     */
    @org.springframework.web.bind.annotation.GetMapping("/count")
    public org.springframework.http.ResponseEntity<com.queuesmart.dto.ApiResponse<Long>> getHistoryCount(
            org.springframework.security.core.Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return org.springframework.http.ResponseEntity.ok(
                com.queuesmart.dto.ApiResponse.success("History count",
                        historyService.countUserHistory(userId)));
    }

}
