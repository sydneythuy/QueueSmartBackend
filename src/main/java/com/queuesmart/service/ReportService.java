package com.queuesmart.service;

import com.queuesmart.model.HistoryRecord;
import com.queuesmart.model.QueueEntry;
import com.queuesmart.repository.HistoryRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final HistoryRecordRepository historyRepo;
    private final HistoryService historyService;

    @Transactional(readOnly = true)
    public String generateFullHistoryCsv(LocalDate from, LocalDate to, String serviceName) {
        List<HistoryRecord> records = historyRepo.findAll().stream()
                .filter(r -> from == null || (r.getJoinedAt() != null &&
                        !r.getJoinedAt().toLocalDate().isBefore(from)))
                .filter(r -> to == null || (r.getJoinedAt() != null &&
                        !r.getJoinedAt().toLocalDate().isAfter(to)))
                .filter(r -> serviceName == null || serviceName.isBlank() ||
                        (r.getServiceName() != null &&
                         r.getServiceName().toLowerCase().contains(serviceName.toLowerCase())))
                .collect(Collectors.toList());

        List<HistoryRecord> served = records.stream()
                .filter(r -> r.getFinalStatus() == QueueEntry.EntryStatus.SERVED).toList();
        List<HistoryRecord> left = records.stream()
                .filter(r -> r.getFinalStatus() == QueueEntry.EntryStatus.LEFT).toList();

        StringBuilder sb = new StringBuilder();
        sb.append("Report generated for: ");
        sb.append(from != null ? "From " + from : "All dates");
        sb.append(to != null ? " to " + to : "");
        sb.append(serviceName != null && !serviceName.isBlank() ? " | Service: " + serviceName : " | All services");
        sb.append("\n\n");

        sb.append("SERVED CUSTOMERS\n");
        sb.append("Record ID,User ID,User Email,Service Name,Joined At,Completed At,Wait (minutes)\n");
        for (HistoryRecord r : served) {
            sb.append(csv(r.getId())).append(",");
            sb.append(csv(r.getUserId())).append(",");
            sb.append(csv(r.getUsername())).append(",");
            sb.append(csv(r.getServiceName())).append(",");
            sb.append(csv(r.getJoinedAt() != null ? r.getJoinedAt().toString() : "")).append(",");
            sb.append(csv(r.getCompletedAt() != null ? r.getCompletedAt().toString() : "")).append(",");
            sb.append(r.getWaitedMinutes()).append("\n");
        }

        sb.append("\nCUSTOMERS WHO LEFT QUEUE\n");
        sb.append("Record ID,User ID,User Email,Service Name,Joined At,Left At,Wait (minutes)\n");
        for (HistoryRecord r : left) {
            sb.append(csv(r.getId())).append(",");
            sb.append(csv(r.getUserId())).append(",");
            sb.append(csv(r.getUsername())).append(",");
            sb.append(csv(r.getServiceName())).append(",");
            sb.append(csv(r.getJoinedAt() != null ? r.getJoinedAt().toString() : "")).append(",");
            sb.append(csv(r.getCompletedAt() != null ? r.getCompletedAt().toString() : "")).append(",");
            sb.append(r.getWaitedMinutes()).append("\n");
        }

        sb.append("\nSUMMARY\n");
        sb.append("Total Served,Total Left,Total Records\n");
        sb.append(served.size()).append(",").append(left.size()).append(",").append(records.size()).append("\n");
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String generateServiceStatsCsv(String serviceName) {
        Map<String, Long> usage = historyService.getUsageStatsByService();
        Map<String, Double> avgWait = historyService.getAverageWaitByService();
        long totalServed = historyService.getTotalServed();

        StringBuilder sb = new StringBuilder();
        sb.append("Service Name,Total Visits,Average Wait (minutes)\n");
        for (String svc : usage.keySet()) {
            if (serviceName != null && !serviceName.isBlank() &&
                !svc.toLowerCase().contains(serviceName.toLowerCase())) continue;
            sb.append(csv(svc)).append(",");
            sb.append(usage.get(svc)).append(",");
            sb.append(String.format("%.1f", avgWait.getOrDefault(svc, 0.0))).append("\n");
        }
        sb.append("\nTotal Served (all services):,").append(totalServed).append("\n");
        return sb.toString();
    }

    private String csv(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
