package com.im.admin.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @Column(name = "bank_transfer_to", nullable = false)
    private String bankTransferTo;

    @Column(name = "bank_holder_name")
    private String bankHolderName;

    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_tran_id")
    @NotBlank(message = "Bank transaction ID is required")
    @Size(min = 5, max = 50, message = "Bank transaction ID must be between 5 and 50 characters")
    private String bankTranId;

    @Column(name = "admin_tran_id")
    private String adminTranId;

    @Column(name = "deposit_date")
    private LocalDate depositDate;

    @Column(name = "narration", length = 500)
    private String narration;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "ip_address")
    @NotBlank(message = "IP address is required")
    private String ipAddress;

    @Column(name = "approve_or_reject_date")
    private LocalDate approveOrRejectDate;

    @Column(name = "approve_or_reject_time")
    private LocalTime approveOrRejectTime;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "deposit_type")
    private String depositType;

    // Relationship with SubProducts
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subProduct_id")
    private SubProducts subProducts;

    // Relationship with Scheme
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id")
    private Scheme scheme;
}

