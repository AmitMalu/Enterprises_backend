package com.im.admin.entities.product;

import jakarta.persistence.*;
import jakarta.validation.Valid;
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
@Table(name = "products")
public class Products {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Uses MySQL auto-increment
    @Column(name = "id", columnDefinition = "TINYINT UNSIGNED")
    private Short id;

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

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "edited_on")
    private LocalDateTime editedOn;

    @Column(name = "edited_by")
    private String editedBy;
}




