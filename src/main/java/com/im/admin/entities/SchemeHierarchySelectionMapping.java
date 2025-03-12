package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scheme_hierarchy_selection_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchemeHierarchySelectionMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "selection_id", nullable = false)
    private SchemeHierarchySelection selection;

    @ManyToOne
    @JoinColumn(name = "hierarchy_id", nullable = false)
    private HierarchyMaster hierarchy;

    @Column(name = "hierarchy_code", nullable = false)
    private String hierarchyCode;
}

