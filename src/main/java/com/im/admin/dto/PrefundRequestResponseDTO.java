package com.im.admin.dto;

/**
 * @author SHUBHAM KHOPADE
 */
import com.im.admin.entities.enums.Status;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PrefundRequestResponseDTO {

    private Long id;
    private String requestedBy;
    private String mobileNumber;
    private Double depositAmount;
    private Status status;

    private LocalDate depositDate;
    private LocalDateTime createDateTime;

    private String bankHolderName;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankTranId;
    private String adminTxnId;

    private String narration;
    private String paymentMode;
    private String ipAddress;

    private LocalDate approveOrRejectDate;
    private LocalTime approveOrRejectTime;

    private String remarks;
    private String depositType;
    private String depositeImage;

    private String subProductName;
    private String productName;
    private String schemeName;
}
