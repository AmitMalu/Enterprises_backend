package com.im.admin.entities;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "sub_products")
public class SubProducts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_product_title", nullable = false)
    private String subProductTitle;

    @Column(name = "product", nullable = false)
    @NotNull(message = "Product cannot be null")
    @NotBlank(message = "Product cannot be blank")
    private String product;

    @Column(name = "sub_product_code", nullable = false)
    @NotNull(message = "Sub Product Code cannot be null")
    @NotBlank(message = "Sub Product Code cannot be blank")
    private String subProductCode;

    @Column(name = "is_settlement")
    @NotNull(message = "Is Settlement is required")
    private Boolean isSettlement;

    @Column(name = "enabled")
    @NotNull(message = "Is Enabled is required")
    private Boolean enabled;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "edited_by")
    private String editedBy;

    @Column(name = "edited_on", nullable = false)
    private LocalDateTime editedOn;

}
