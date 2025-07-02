package com.im.admin.response;


import com.im.admin.entities.SchemeHierarchySelection;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SchemeHierarchySelectionResponseDTO {

    private String id;
    private String schemeSelectionHierarchyName;
    private String schemeHierarchyCombination;
    private String combinationHashCode;

    private String createdBy;
    private LocalDateTime createdOn;
    private String editedBy;
    private LocalDateTime editedOn;

    public SchemeHierarchySelectionResponseDTO(SchemeHierarchySelection entity) {
        this.id = entity.getId();
        this.schemeSelectionHierarchyName = entity.getSchemeSelectionHierarchyName();
        this.schemeHierarchyCombination = entity.getSchemeHierarchyCombination();
        this.combinationHashCode = entity.getCombinationHashCode();
        this.createdBy = entity.getCreatedBy();
        this.createdOn = entity.getCreatedOn();
        this.editedBy = entity.getEditedBy();
        this.editedOn = entity.getEditedOn();
    }
}
