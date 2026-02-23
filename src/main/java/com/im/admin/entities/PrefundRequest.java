package com.im.admin.entities;

import com.im.admin.entities.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "prefund_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "mobile_number", nullable = false)
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number format")
    private String mobileNumber;

    @Column(name = "deposit_amount", nullable = false)
    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "0.1", inclusive = true, message = "Deposit amount must be greater than zero")
    private Double depositAmount;

    @Column(name = "deposit_image")
    private String depositImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createDateTime;

    @Column(name = "bank_holder_name")
    @NotBlank(message = "Bank holder name is required")
    @Size(min = 2, max = 100, message = "Bank holder name must be between 2 and 100 characters")
    private String bankHolderName;

    @Column(name = "bank_account_name")
    @NotBlank(message = "Bank account name is required")
    @Size(min = 2, max = 100, message = "Bank account name must be between 2 and 100 characters")
    private String bankAccountName;

    @Column(name = "bank_account_number")
    @NotBlank(message = "Bank account number is required")
    @Pattern(regexp = "\\d{9,18}", message = "Bank account number must be between 9 to 18 digits")
    private String bankAccountNumber;

    @Column(name = "bank_tran_id")
    @NotBlank(message = "Bank transaction ID is required")
    @Size(min = 5, max = 50, message = "Bank transaction ID must be between 5 and 50 characters")
    private String bankTranId;

    @Column(name = "deposit_date")
    private LocalDate depositDate;

    @Column(name = "narration", length = 500)
    private String narration;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "ip_address")
    @NotBlank(message = "IP address is required")
//    @Pattern(
//            regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$",
//            message = "Invalid IP address format"
//    )
    private String ipAddress;

    @Column(name = "approve_or_reject_date")
    private LocalDate approveOrRejectDate;

    @Column(name = "approve_or_reject_time")
    private LocalTime approveOrRejectTime;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "deposit_type")
    private String depositType;
}

