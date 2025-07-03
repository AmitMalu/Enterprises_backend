package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MenuWithSubMenuResponseDto {
    private String id;
    private String name;
    private List<SubMenuShortDto> submenus;

}
