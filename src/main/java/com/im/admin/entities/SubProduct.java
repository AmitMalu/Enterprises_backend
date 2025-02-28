package com.im.admin.entities;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "subproduct")
@Data
public class SubProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "product", nullable = false)
    private String product;

    @Column(name = "sub_product", nullable = false)
    private String subProduct;

    @Column(name = "is_settlement")
    private Boolean isSettlement;

    @Column(name = "blocked")
    private Boolean blocked;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "edited_by")
    private String editedBy;

    @Column(name = "edited_on")
    private LocalDateTime editedOn;

}
