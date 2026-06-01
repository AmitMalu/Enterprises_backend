package com.im.admin.cronJob;

import com.im.admin.service.impl.BillAvenueStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Slf4j
public class BillAvenueStatusScheduler {

    @Autowired
    private BillAvenueStatusService billAvenueStatusService;

//    @Scheduled(cron = "0 */5 * * * *") // every 5 minutes
      @Scheduled(cron = "0 0 * * * *") //every one hour
    public void checkBillAvenuePendingTxns() {
        log.info("BillAvenue cron started...");
        billAvenueStatusService.checkPendingTransactions();
    }
}
