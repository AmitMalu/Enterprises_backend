package com.im.admin.mapper;

import org.springframework.stereotype.Component;

/**
 * @author SHUBHAM KHOPADE
 */
@Component
public class StatusMapper {

    public static String mapIMStatusToInternalStatus(String status) {
        if (status == null) {
            return "PENDING";
        }

        return switch (status.toLowerCase()) {
            case "completed" -> "SUCCESS";
            case "failed" -> "FAILED";
            default -> "PENDING"; // waiting or any other status
        };
    }
}
