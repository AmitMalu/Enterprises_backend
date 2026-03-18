package com.im.admin.cronJob;

import com.im.admin.orchestrator.PayoutOrchestrator;
import com.im.admin.service.PayoutCheckStatusSchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@EnableScheduling
@Component
@Slf4j
public class PayoutCheckStatusScheduler {

    @Autowired
    private PayoutCheckStatusSchedulerService payoutCheckStatusSchedulerService;

    @Autowired
    private PayoutOrchestrator payoutOrchestrator;

    @Scheduled(cron = "0 */59 * * * *")  // every 10 minutes
    @Transactional
    public void runPendingTransactionCheck() {
        log.info("=== [SCHEDULER] Checking vendor payout statuses ===");
        payoutOrchestrator.checkStatusForPendingTransactions();
    }

}
