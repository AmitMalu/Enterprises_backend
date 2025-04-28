package com.im.admin.entities;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.im.admin.entities.enums.Interval;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "charge_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChargeMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Name is mandatory")
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @NotNull(message = "Interval is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "interval_code", nullable = false, length = 2)
    private Interval interval;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    @Column(name = "amount")
    private Double amount;

    @NotNull(message = "GST is required")
    @JsonProperty("GST")
    @DecimalMin(value = "0.0", inclusive = true, message = "GST must be 0 or greater")
    @DecimalMax(value = "100.0", inclusive = true, message = "GST cannot exceed 100%")
    @Column(name = "gst")
    private Double gst;

    @Column(name = "total_amount")
    private Double totalAmount;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

}
