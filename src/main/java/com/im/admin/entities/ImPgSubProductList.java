package com.im.admin.entities;

/**
 * @author SHUBHAM KHOPADE
 */
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "im_subProduct",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pg_settlement_excel_id",
                        columnNames = "excel_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImPgSubProductList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * ID received from Excel.
     * Kept as String so leading zeroes are not removed.
     */
    @Column(name = "excel_id", nullable = false, length = 100)
    private String excelId;

    @Column(name = "product", nullable = false, length = 100)
    private String product;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "pg_code", nullable = false, length = 100)
    private String pgCode;

    @Column(name = "settlement_category", nullable = false, length = 255)
    private String settlementCategory;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;
}
