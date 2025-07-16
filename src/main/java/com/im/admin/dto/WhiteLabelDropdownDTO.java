package com.im.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WhiteLabelDropdownDTO {

    private String whiteLabelId;
    private String displayName;

    public WhiteLabelDropdownDTO(String whiteLabelId, String name) {
        this.whiteLabelId = whiteLabelId;
        this.displayName = name + " #" + whiteLabelId;
    }

}
