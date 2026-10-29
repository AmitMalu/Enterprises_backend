package com.scs.admin.service;

import com.scs.admin.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MenuService {
    String addMenu(MenuRequestDto menuRequestDto);

    Page<MenuResponseDto> getAllMenus(int page, int size, Integer menuId, String menuName);

    MenuResponseDto getMenuById(String menuId);

    String updateMenu(String menuId, MenuRequestDto requestDto);

    String addSubMenu(SubMenuRequestDto dto);

    Page<SubMenuResponseDto> getAllSubMenus(int page, int size, Integer menuId, String subMenuName);

    String updateSubMenu(String subMenuId, SubMenuRequestDto dto);

    SubMenuResponseDto getSubMenuById(String subMenuId);

    List<MenuWithSubMenuResponseDto> getMenusWithSubMenus();

}
