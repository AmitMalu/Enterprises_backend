package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MenuWithSubMenuResponseDto {
    private Integer id;
    private String name;
    private List<SubMenuShortDto> submenus;

}
