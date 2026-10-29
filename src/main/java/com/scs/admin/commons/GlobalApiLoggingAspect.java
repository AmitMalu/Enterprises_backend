package com.scs.admin.commons;

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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalApiLoggingAspect {

    private final CommonFunctions commonFunctions;
    private final ObjectMapper objectMapper;

    @Around("@within(com.scs.admin.commons.LoggableApi)")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {

        LocalDateTime startTime = LocalDateTime.now();
        String request = "";
        String response = "";
        String vendorId = VendorContext.getVendorId();

        try {
            Object[] safeArgs = Arrays.stream(joinPoint.getArgs())
                    .filter(arg -> !(arg instanceof HttpServletRequest))
                    .filter(arg -> !(arg instanceof HttpServletResponse))
                    .filter(arg -> !(arg instanceof MultipartFile))
                    .toArray();

            request = objectMapper.writeValueAsString(safeArgs);

            Object result = joinPoint.proceed();

            response = result != null
                    ? objectMapper.writeValueAsString(result)
                    : "null";

            return result;

        } catch (Exception ex) {
            response = "ERROR: " + ex.getMessage();
            throw ex;

        } finally {

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
