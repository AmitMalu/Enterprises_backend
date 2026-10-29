package com.scs.admin.cronJob;

import com.scs.admin.service.impl.RechargeStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableScheduling
@Slf4j
public class BBPSRechargeCheckStatusScheduler {

    @Autowired
    private RechargeStatusService rechargeStatusService;

    @Scheduled(cron = "0 */59 * * * *")  // every 5 minutes
    @Transactional
    public void processPendingTransactions() {
        log.info("Cron job triggered to check pending recharge transactions...");
        rechargeStatusService.checkStatusForPendingTransactions();
    }
}
