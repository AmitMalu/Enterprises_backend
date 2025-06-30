package com.im.admin.service;

import com.im.admin.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MenuService {
    String addMenu(MenuRequestDto menuRequestDto);

    Page<MenuResponseDto> getAllMenus(int page, int size, Integer menuId, String menuName);

    MenuResponseDto getMenuById(Integer menuId);

    String updateMenu(Integer menuId, MenuRequestDto requestDto);

    String addSubMenu(SubMenuRequestDto dto);

    Page<SubMenuResponseDto> getAllSubMenus(int page, int size, Integer menuId, String subMenuName);

    String updateSubMenu(Integer subMenuId, SubMenuRequestDto dto);

    SubMenuResponseDto getSubMenuById(Integer subMenuId);

    List<MenuWithSubMenuResponseDto> getMenusWithSubMenus();

}
