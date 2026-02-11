package com.im.admin.entities;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "scheme")
@AllArgsConstructor
@NoArgsConstructor
public class Scheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheme_name")
    @NotBlank(message = "Scheme Name is Required")
    private String schemeName;

    @Column(name = "scheme_code")
    @NotBlank(message = "Scheme Code is Required")
    private String schemeCode;

    @Column(name = "scheme_combination_hashCode")
    private String schemeCombinationHashCode;    //10- productId,-01 sub-productId, -02 vendorId, -001 schemeHierarchySelectionId,00001 and this is generated Id number for table itself
                                                    // final hashcode (10-01-02-001-00001)

//    @Column(name = "is_active")
//    private Boolean isActive = true;

    @Column(name = "is_active")
    private Boolean isActive;

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
