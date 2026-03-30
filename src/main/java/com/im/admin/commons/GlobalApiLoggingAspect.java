package com.im.admin.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author SHUBHAM KHOPADE
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalApiLoggingAspect {

    private final CommonFunctions commonFunctions;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@within(com.im.admin.commons.LoggableApi)")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {

        LocalDateTime startTime = LocalDateTime.now();
        String request = "";
        String response = "";
        String vendorId = VendorContext.getVendorId();

        try {
            request = objectMapper.writeValueAsString(joinPoint.getArgs());

            Object result = joinPoint.proceed();

            response = result != null ? objectMapper.writeValueAsString(result) : "null";

            return result;

        } catch (Exception ex) {
            response = "ERROR: " + ex.getMessage();
            throw ex;

        } finally {

            // Only save if ServiceImpl did NOT execute
            if (!VendorContext.isServiceCalled()) {

                commonFunctions.saveVendorLogs(
                        startTime,
                        request,
                        vendorId,
                        LocalDateTime.now(),
                        response,
                        null
                );
            }

            VendorContext.clear();

        }
    }
}
