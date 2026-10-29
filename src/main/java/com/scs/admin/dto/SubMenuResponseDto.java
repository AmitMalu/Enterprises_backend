package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubMenuResponseDto {
    private String subMenuId;
    private String menuId;
    private String subMenuName;
}
