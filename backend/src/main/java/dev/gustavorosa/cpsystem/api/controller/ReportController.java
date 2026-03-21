package dev.gustavorosa.cpsystem.api.controller;

import dev.gustavorosa.cpsystem.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/report")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/client/{clientId}/monthly")
    public ResponseEntity<byte[]> generateMonthlyReport(
            @PathVariable Long clientId,
            @RequestParam int month,
            @RequestParam int year) {

        log.info("[ReportController] - Generating monthly report for client {} - {}/{}", clientId, month, year);

        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest().build();
        }
        if (year < 2000 || year > 2100) {
            return ResponseEntity.badRequest().build();
        }

        byte[] pdf = reportService.generateMonthlyReport(clientId, month, year);

        String filename = String.format("relatorio_%d_%02d_%d.pdf", clientId, month, year);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
