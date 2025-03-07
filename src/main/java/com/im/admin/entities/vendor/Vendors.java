package com.im.admin.entities.vendor;

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
@Table(name = "vendors")
public class Vendors {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(2)")
    private String id;

    @Embedded
    @Valid
    private Info info;

    @Embedded
    @Valid
    private VendorAccount vendorAccount;

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
