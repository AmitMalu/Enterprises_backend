package com.scs.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sub_operators")
public class SubOperator {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(2)")
    private String id;

    @Column(name = "operator_code")
    private String operatorCode;

    @Column(name = "operator_alias")
    private String operatorAlias;

    @NotNull(message = "Operator Name is required")
    @Column(name = "operator_name", nullable = false)
    private String operatorName;

    @NotNull(message = "Sub Operator Name is required")
    @Column(name = "sub_operator_name", nullable = false)
    private String subOperatorName;

    @Column(name = "category")
    private String category;

    @Column(name = "is_blocked")
    private Boolean isBlocked;

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

