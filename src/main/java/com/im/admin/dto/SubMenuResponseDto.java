package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubMenuResponseDto {
    private Integer subMenuId;
    private Integer menuId;
    private String subMenuName;
}
