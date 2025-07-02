package com.im.admin.response;


import com.im.admin.entities.SchemeHierarchySelection;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Data
public class SchemeHierarchySelectionResponseDTO {

    private String id;
    private String schemeHierarchySelectionName;
    private List<String> schemeHierarchyCombination;
    private String combinationHashCode;

    private String createdBy;
    private LocalDateTime createdOn;
    private String editedBy;
    private LocalDateTime editedOn;

    public SchemeHierarchySelectionResponseDTO(SchemeHierarchySelection entity) {
        this.id = entity.getId();
        this.schemeHierarchySelectionName = entity.getSchemeSelectionHierarchyName();
        this.schemeHierarchyCombination = Arrays.asList(
                Optional.ofNullable(entity.getSchemeHierarchyCombination())
                        .orElse("")
                        .split("-")
        );
        this.combinationHashCode = entity.getCombinationHashCode();
        this.createdBy = entity.getCreatedBy();
        this.createdOn = entity.getCreatedOn();
        this.editedBy = entity.getEditedBy();
        this.editedOn = entity.getEditedOn();
    }

}
