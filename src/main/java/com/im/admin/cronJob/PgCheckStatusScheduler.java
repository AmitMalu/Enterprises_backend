package com.im.admin.cronJob;

import com.im.admin.orchestrator.PGOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author SHUBHAM KHOPADE
 */

@EnableScheduling
@Component
@Slf4j
public class PgCheckStatusScheduler {

    @Autowired
    private PGOrchestrator pgOrchestrator;


    @Scheduled(cron = "0 */59 * * * *")  // every 59 minutes
    @Transactional
    public void runPendingTransactionCheck() {
        log.info("=== [SCHEDULER] Checking vendor payment gateway statuses ===");
        pgOrchestrator.checkStatusForPendingTransactions();
    }
}
