package dev.gustavorosa.cpsystem.api.controller;

import dev.gustavorosa.cpsystem.api.response.RecalculateOverdueInterestResponse;
import dev.gustavorosa.cpsystem.scheduled.UpdatePaymentStatusJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/jobs")
@Slf4j
public class JobController {

    @Autowired
    private UpdatePaymentStatusJob jobService;

    @PostMapping("/recalculate-overdue-interest")
    public RecalculateOverdueInterestResponse recalculateOverdueInterest() {
        log.info("Manual trigger: recalculating overdue interest...");
        int marked = jobService.updatePaymentStatus();
        int recalculated = jobService.updatePaymentOverdueValues();
        return new RecalculateOverdueInterestResponse(marked, recalculated);
    }
}
