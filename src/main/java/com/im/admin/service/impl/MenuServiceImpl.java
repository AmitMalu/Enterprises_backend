package com.im.admin.service.impl;

import com.im.admin.dto.*;
import com.im.admin.entities.Menu;
import com.im.admin.entities.SubMenu;
import com.im.admin.repositories.MenuRepository;
import com.im.admin.repositories.SubMenuRepository;
import com.im.admin.service.MenuService;
import com.im.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private SubMenuRepository subMenuRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Override
    public String addMenu(MenuRequestDto menuRequestDto) {

        Optional<Menu> existing = menuRepository.findByMenuName(menuRequestDto.getMenuName());

        if (existing.isPresent()) {
            return "Menu already exists.";
        }

        Menu menu = new Menu();

        //  custom ID generation logic
        String nextId = commonMethods.generateNextIdForTables("menus", "menu_id");
        menu.setMenuId(nextId);
        menu.setMenuName(menuRequestDto.getMenuName());
        menuRepository.save(menu);

        return "Menu added successfully.";
    }

    @Override
    public Page<MenuResponseDto> getAllMenus(int page, int size, Integer menuId, String menuName) {
        List<Menu> allMenus = menuRepository.findAll();

        List<MenuResponseDto> filtered = allMenus.stream()
                .filter(menu -> menuId == null || menu.getMenuId().equals(menuId))
                .filter(menu -> menuName == null || (menu.getMenuName() != null &&
                        menu.getMenuName().toLowerCase().contains(menuName.toLowerCase())))
                .map(menu -> new MenuResponseDto(menu.getMenuId(), menu.getMenuName()))
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());

        List<MenuResponseDto> paginated = filtered.subList(start, end);
        return new PageImpl<>(paginated, pageable, filtered.size());
    }

    @Override
    public MenuResponseDto getMenuById(String menuId) {
        Optional<Menu> optionalMenu = menuRepository.findById(menuId);
        if (optionalMenu.isEmpty()) {
            return null;
        }

        Menu menu = optionalMenu.get();
        return new MenuResponseDto(menu.getMenuId(), menu.getMenuName());
    }

    @Override
    public String updateMenu(String menuId, MenuRequestDto dto) {
        Optional<Menu> menuOptional = menuRepository.findById(menuId);

        if (menuOptional.isEmpty()) {
            return "Menu not found.";
        }

        Menu menu = menuOptional.get();
        menu.setMenuName(dto.getMenuName());

        menuRepository.save(menu);
        return "Menu updated successfully.";
    }

    @Override
    public String addSubMenu(SubMenuRequestDto dto) {
        // Check if Menu exists
        Menu menu = menuRepository.findById(dto.getMenuId())
                .orElseThrow(() -> new RuntimeException("Menu not found with ID: " + dto.getMenuId()));

        // Check if SubMenu already exists for the menu
        boolean exists = subMenuRepository.existsByMenuIdAndSubMenuName(dto.getMenuId(), dto.getSubMenuName());
        if (exists) {
            return "Sub menu already exists.";
        }

        // Create and save new SubMenu
        SubMenu subMenu = new SubMenu();

        //  custom ID generation logic
        String nextId = commonMethods.generateNextIdForTables("sub_menus", "sub_menu_id");
        subMenu.setSubMenuId(nextId);
        subMenu.setMenuId(dto.getMenuId());
        subMenu.setSubMenuName(dto.getSubMenuName());

        subMenuRepository.save(subMenu);
        return "Sub menu added successfully.";
    }

    @Override
    public Page<SubMenuResponseDto> getAllSubMenus(int page, int size, Integer menuId, String subMenuName) {
        List<SubMenu> all = subMenuRepository.findAll();

        List<SubMenuResponseDto> filtered = all.stream()
                .filter(sm -> menuId == null || sm.getMenuId().equals(menuId))
                .filter(sm -> subMenuName == null || (sm.getSubMenuName() != null &&
                        sm.getSubMenuName().toLowerCase().contains(subMenuName.toLowerCase())))
                .map(sm -> new SubMenuResponseDto(sm.getSubMenuId(), sm.getMenuId(), sm.getSubMenuName()))
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());

        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @Override
    public SubMenuResponseDto getSubMenuById(String subMenuId) {
        return subMenuRepository.findById(subMenuId)
                .map(sm -> new SubMenuResponseDto(sm.getSubMenuId(), sm.getMenuId(), sm.getSubMenuName()))
                .orElse(null);
    }

    @Override
    public String updateSubMenu(String subMenuId, SubMenuRequestDto dto) {
        Optional<SubMenu> optional = subMenuRepository.findById(subMenuId);
        if (optional.isEmpty()) {
            return "Sub menu not found.";
        }

        SubMenu subMenu = optional.get();
        subMenu.setMenuId(dto.getMenuId());
        subMenu.setSubMenuName(dto.getSubMenuName());

        subMenuRepository.save(subMenu);
        return "Sub menu updated successfully.";
    }

    @Override
    public List<MenuWithSubMenuResponseDto> getMenusWithSubMenus() {
        List<Menu> menus = menuRepository.findAll();
        List<SubMenu> subMenus = subMenuRepository.findAll();

        // Group submenus by menuId
        Map<String, List<SubMenu>> subMenuMap = subMenus.stream()
                .collect(Collectors.groupingBy(SubMenu::getMenuId));

        // Build the response list
        return menus.stream()
                .map(menu -> {
                    List<SubMenuShortDto> subList = subMenuMap
                            .getOrDefault(menu.getMenuId(), new ArrayList<>())
                            .stream()
                            .map(sm -> new SubMenuShortDto(
                                    sm.getSubMenuId(),
                                    sm.getSubMenuName(),
                                    menu.getMenuId() + sm.getSubMenuId() //  combineId logic
                            ))
                            .collect(Collectors.toList());

                    return new MenuWithSubMenuResponseDto(
                            menu.getMenuId(),
                            menu.getMenuName(),
                            subList
                    );
                })
                .collect(Collectors.toList());
    }


}


