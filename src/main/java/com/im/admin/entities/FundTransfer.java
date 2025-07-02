package com.im.admin.entities;


import com.im.admin.entities.enums.WalletType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "fund_transfer")
@NoArgsConstructor
@AllArgsConstructor
public class FundTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "From hierarchy ID must not be blank")
    @Column(name = "from_hierarchy_id")
    private String fromHierarchyId;

    @NotBlank(message = "To hierarchy ID must not be blank")
    @Column(name = "to_hierarchy_id")
    private String toHierarchyId;

    @NotNull(message = "Amount must not be null")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than zero")
    @Column(name = "amount")
    private BigDecimal amount;

    @NotNull(message = "From wallet type must not be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "from_wallet_type")
    private WalletType fromWalletType;

    @NotNull(message = "To wallet type must not be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "to_wallet_type")
    private WalletType toWalletType;

    @CreationTimestamp
    @Column(name = "transfer_date", updatable = false)
    private LocalDateTime transferDate;

    @Size(max = 255, message = "Narration must be 255 characters or less")
    @Column(name = "narration")
    private String narration;
}

