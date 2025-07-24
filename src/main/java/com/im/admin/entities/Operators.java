package com.im.admin.entities;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "operators")
public class Operators {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(2)")
    private String id;

    @NotBlank(message = "Operator Category is required")
    @Column(name = "operator_category", nullable = false)
    private String operatorCategory;

    @NotBlank(message = "Operator Code is required")
    @Column(name = "operator_code", nullable = false)
    private String operatorCode;

    @NotBlank(message = "Operator Name is required")
    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "operator_alias")
    private String operatorAlias;

    @Column(name = "alias")
    private String alias;

    @NotNull(message = "Product is required")
    @Column(name = "product_id")
    private String productId;

    @Column(name = "product_code")
    private String productCode;

    @NotNull(message = "Sub Product is required")
    @Column(name = "sub_product_id")
    private String subProductId;

    @Column(name = "sub_product_code")
    private String subProductCode;

    @Column(name = "card_bin_no")
    private String cardBinNo;

    @NotNull(message = "Active status is required")
    private Boolean isActive = false;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "edited_on")
    private LocalDateTime editedOn;

    @Column(name = "edited_by")
    private String editedBy;

}


