package com.queuesmart.service;

import com.queuesmart.model.HistoryRecord;
import com.queuesmart.model.QueueEntry;
import com.queuesmart.model.UserCredential;
import com.queuesmart.repository.HistoryRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private HistoryRecordRepository historyRepo;
    @Mock private HistoryService historyService;

    @InjectMocks private ReportService reportService;

    private HistoryRecord makeRecord(String id, String serviceName,
                                     QueueEntry.EntryStatus status, int daysAgo) {
        UserCredential user = UserCredential.builder()
                .id("u1").email("alice@example.com").build();
        return HistoryRecord.builder()
                .id(id).user(user).serviceName(serviceName)
                .joinedAt(LocalDateTime.now().minusDays(daysAgo))
                .completedAt(LocalDateTime.now().minusDays(daysAgo).plusMinutes(10))
                .finalStatus(status).waitedMinutes(10).build();
    }

    @Test
    void generateFullHistoryCsv_NoFilters_IncludesAllRecords() {
        when(historyRepo.findAll()).thenReturn(List.of(
                makeRecord("h1", "Advising", QueueEntry.EntryStatus.SERVED, 1),
                makeRecord("h2", "Clinic", QueueEntry.EntryStatus.LEFT, 2)
        ));

        String csv = reportService.generateFullHistoryCsv(null, null, null);

        assertTrue(csv.contains("SERVED CUSTOMERS"));
        assertTrue(csv.contains("CUSTOMERS WHO LEFT QUEUE"));
        assertTrue(csv.contains("Advising"));
        assertTrue(csv.contains("Clinic"));
        assertTrue(csv.contains("Total Served,Total Left,Total Records"));
        assertTrue(csv.contains("1,1,2"));
    }

    @Test
    void generateFullHistoryCsv_WithServiceFilter_OnlyMatchingService() {
        when(historyRepo.findAll()).thenReturn(List.of(
                makeRecord("h1", "Advising", QueueEntry.EntryStatus.SERVED, 1),
                makeRecord("h2", "Clinic", QueueEntry.EntryStatus.SERVED, 2)
        ));

        String csv = reportService.generateFullHistoryCsv(null, null, "Advising");

        assertTrue(csv.contains("Advising"));
        assertFalse(csv.contains("Clinic"));
        assertTrue(csv.contains("Service: Advising"));
    }

    @Test
    void generateFullHistoryCsv_WithDateFilter_OnlyRecordsInRange() {
        when(historyRepo.findAll()).thenReturn(List.of(
                makeRecord("h1", "Advising", QueueEntry.EntryStatus.SERVED, 1),  // yesterday
                makeRecord("h2", "Clinic", QueueEntry.EntryStatus.SERVED, 30)    // 30 days ago
        ));

        LocalDate from = LocalDate.now().minusDays(7);
        String csv = reportService.generateFullHistoryCsv(from, null, null);

        assertTrue(csv.contains("Advising"));
        assertFalse(csv.contains("Clinic"));
    }

    @Test
    void generateServiceStatsCsv_NoFilter_IncludesAllServices() {
        when(historyService.getUsageStatsByService())
                .thenReturn(Map.of("Advising", 5L, "Clinic", 3L));
        when(historyService.getAverageWaitByService())
                .thenReturn(Map.of("Advising", 12.5, "Clinic", 8.0));
        when(historyService.getTotalServed()).thenReturn(8L);

        String csv = reportService.generateServiceStatsCsv(null);

        assertTrue(csv.contains("Advising"));
        assertTrue(csv.contains("Clinic"));
        assertTrue(csv.contains("Total Served (all services):,8"));
    }

    @Test
    void generateServiceStatsCsv_WithFilter_OnlyMatchingService() {
        when(historyService.getUsageStatsByService())
                .thenReturn(Map.of("Advising", 5L, "Clinic", 3L));
        when(historyService.getAverageWaitByService())
                .thenReturn(Map.of("Advising", 12.5, "Clinic", 8.0));
        when(historyService.getTotalServed()).thenReturn(8L);

        String csv = reportService.generateServiceStatsCsv("Advising");

        assertTrue(csv.contains("Advising"));
        assertFalse(csv.contains("Clinic"));
    }

    @Test
    void generateFullHistoryCsv_EmptyRecords_StillHasHeaders() {
        when(historyRepo.findAll()).thenReturn(List.of());

        String csv = reportService.generateFullHistoryCsv(null, null, null);

        assertTrue(csv.contains("SERVED CUSTOMERS"));
        assertTrue(csv.contains("CUSTOMERS WHO LEFT QUEUE"));
        assertTrue(csv.contains("0,0,0"));
    }
}
