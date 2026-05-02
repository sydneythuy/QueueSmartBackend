package com.queuesmart.controller;

import com.queuesmart.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    /**
     * GET /api/reports/history/csv?from=2024-01-01&to=2024-12-31&service=Advising
     * All params optional. Downloads filtered history CSV.
     */
    @GetMapping("/history/csv")
    public ResponseEntity<byte[]> downloadHistoryCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String service) {

        String csv = reportService.generateFullHistoryCsv(from, to, service);
        return csvResponse(csv, "queue_history_" + timestamp() + ".csv");
    }

    /**
     * GET /api/reports/stats/csv?service=Advising
     * Downloads service stats CSV, optionally filtered by service name.
     */
    @GetMapping("/stats/csv")
    public ResponseEntity<byte[]> downloadStatsCsv(
            @RequestParam(required = false) String service) {

        String csv = reportService.generateServiceStatsCsv(service);
        return csvResponse(csv, "queue_stats_" + timestamp() + ".csv");
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(bytes.length)
                .body(bytes);
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
}
