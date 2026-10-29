package com.scs.admin.commons;

/**
 * @author SHUBHAM KHOPADE
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class VendorServiceLoggingAspect {

    private final CommonFunctions commonFunctions;
    private final ObjectMapper objectMapper;

    // ONLY SERVICEIMPL
    @Around("@within(com.scs.admin.commons.LoggableVendor)")
    public Object logVendorService(ProceedingJoinPoint joinPoint) throws Throwable {

        LocalDateTime startTime = LocalDateTime.now();

        String request = "";
        String response = "";

        String vendorId = VendorContext.getVendorId();

        try {

            VendorContext.markServiceCalled();

            // ===== FILTER NON SERIALIZABLE OBJECTS =====
            Object[] filteredArgs = Arrays.stream(joinPoint.getArgs())
                    .filter(arg -> !(arg instanceof ServletRequest))
                    .filter(arg -> !(arg instanceof ServletResponse))
                    .toArray();

            request = objectMapper.writeValueAsString(filteredArgs);

            log.info("Vendor Service Request : {}", request);

            Object result = joinPoint.proceed();

            response = result != null
                    ? objectMapper.writeValueAsString(result)
                    : "null";

            log.info("Vendor Service Response : {}", response);

            return result;

        } catch (Exception ex) {

            response = "ERROR: " + ex.getMessage();

            log.error("Vendor Service Exception", ex);

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