package com.scs.admin.dto;

/**
 * @author SHUBHAM KHOPADE
 */

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestHeaderMetaDto {

    private Double latitude;
    private Double longitude;
    private String ipAddress;
    private String requestDomainName;
    private String medium;

    public static RequestHeaderMetaDto from(HttpServletRequest request) {
        return RequestHeaderMetaDto.builder()
                .latitude(parseDouble(request.getHeader("Latitude")))
                .longitude(parseDouble(request.getHeader("Longitude")))
                .ipAddress(resolveIpAddress(request))
                .requestDomainName(request.getHeader("X-Tenant-Domain"))
                .medium(request.getHeader("Medium"))
                .build();
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String resolveIpAddress(HttpServletRequest request) {

        String ip = request.getHeader("IpAddress");

        if (ip != null && !ip.isBlank()) {
            return ip;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}