package com.im.admin.entities.product;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Products {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @Valid
    private ProductInfo productInfo;

    @Embedded
    @Valid
    private Inventory inventory;

    @Embedded
    @Valid
    private Cluster cluster;

    @Embedded
    @Valid
    private Account account;
}

