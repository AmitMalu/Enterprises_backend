package com.scs.admin.entities;

/**
 * @author SHUBHAM KHOPADE
 */
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class AccessHierarchyPathId implements Serializable {

    @Column(name = "ancestor_id", length = 20)
    private String ancestorId;

    @Column(name = "descendant_id", length = 20)
    private String descendantId;
}
