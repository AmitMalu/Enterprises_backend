package com.im.admin.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "scheme_hierarchy_selection", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"combination_hash"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchemeHierarchySelection {

    @Id
    @Column(name = "id", length = 4, nullable = false, unique = true)
    private String id;

    @Column(name = "combination_hash", nullable = false, unique = true)
    private String combinationHash;

    @OneToMany(mappedBy = "selection", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<SchemeHierarchySelectionMapping> selectedHierarchies = new HashSet<>();

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
