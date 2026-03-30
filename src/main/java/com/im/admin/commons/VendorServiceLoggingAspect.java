package com.im.admin.commons;

/**
 * @author SHUBHAM KHOPADE
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class VendorServiceLoggingAspect {

    private final CommonFunctions commonFunctions;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ONLY SERVICEIMPL
    @Around("@within(com.im.admin.commons.LoggableVendor)")
    public Object logVendorService(ProceedingJoinPoint joinPoint) throws Throwable {

        LocalDateTime startTime = LocalDateTime.now();
        String request = "";
        String response = "";
        String vendorId = VendorContext.getVendorId();

        try {
            VendorContext.markServiceCalled();

            request = objectMapper.writeValueAsString(joinPoint.getArgs());

            Object result = joinPoint.proceed();

            response = result != null ? objectMapper.writeValueAsString(result) : "null";

            return result;

        } catch (Exception ex) {

            response = "ERROR: " + ex.getMessage();
            throw ex;

        } finally {

            try {

                    commonFunctions.saveVendorLogs(
                            startTime,
                            request,
                            vendorId,
                            LocalDateTime.now(),
                            response,
                            null
                    );

            } catch (Exception e) {
                log.error("Error saving vendor logs", e);
            } finally {
                VendorContext.clear();
            }
        }
    }
}
