package com.scs.admin.entities.product;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class Cluster {

    @NotNull(message = "Surcharge status is required")
    @Column(name = "surcharge", nullable = false)
    private Boolean surcharge;

    @NotNull(message = "Scheme engine status is required")
    @Column(name = "scheme_engine", nullable = false)
    private Boolean schemeEngine;
}
