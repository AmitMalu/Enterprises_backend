package com.scs.admin.controller;

import com.scs.admin.dto.*;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.MenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @PostMapping
    public ResponseEntity<CommonResponse> addMenu(@RequestBody MenuRequestDto menuRequestDto) {
        log.info("API Call: addMenu with request: {}", menuRequestDto);
        String result = menuService.addMenu(menuRequestDto);

        if ("Menu already exists.".equals(result)) {
            log.warn("Menu already exists: {}", menuRequestDto);
            return ResponseEntity
                    .status(409)
                    .body(new CommonResponse(409, result, null));
        }

        log.info("Menu added successfully: {}", menuRequestDto);
        return ResponseEntity
                .status(201)
                .body(new CommonResponse(201, result, null));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<MenuResponseDto>>> getAllMenus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer menuId,
            @RequestParam(required = false) String menuName
    ) {
        log.info("API Call: getAllMenus with page: {}, size: {}, menuId: {}, menuName: {}",
                page, size, menuId, menuName);
        Page<MenuResponseDto> result = menuService.getAllMenus(page, size, menuId, menuName);
        log.info("Fetched all menus successfully");
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", result));
    }

    @GetMapping("/{menuId}")
    public ResponseEntity<CommonResponse<MenuResponseDto>> getMenuById(@PathVariable String menuId) {
        log.info("API Call: getMenuById with menuId: {}", menuId);
        MenuResponseDto dto = menuService.getMenuById(menuId);

        if (dto == null) {
            log.warn("Menu not found: {}", menuId);
            return ResponseEntity.status(404).body(new CommonResponse<>(404, "Menu not found", null));
        }

        log.info("Fetched menu successfully for menuId: {}", menuId);
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", dto));
    }

    @PutMapping("/{menuId}")
    public ResponseEntity<CommonResponse> updateMenu(
            @PathVariable String menuId,
            @RequestBody MenuRequestDto requestDto) {

        log.info("API Call: updateMenu with menuId: {}, request: {}", menuId, requestDto);
        String result = menuService.updateMenu(menuId, requestDto);

        if ("Menu not found.".equals(result)) {
            log.warn("Menu not found: {}", menuId);
            return ResponseEntity.status(404).body(new CommonResponse(404, result, null));
        }

        log.info("Menu updated successfully: {}", menuId);
        return ResponseEntity.ok(new CommonResponse(200, result, null));
    }

    @PostMapping("/submenu")
    public ResponseEntity<CommonResponse> addSubMenu(@RequestBody SubMenuRequestDto dto) {
        log.info("API Call: addSubMenu with request: {}", dto);
        String result = menuService.addSubMenu(dto);

        if ("Sub menu already exists.".equals(result)) {
            log.warn("Sub menu already exists: {}", dto);
            return ResponseEntity.status(409).body(new CommonResponse(409, result, null));
        }

        log.info("Sub menu added successfully: {}", dto);
        return ResponseEntity.ok(new CommonResponse(201, result, null));
    }

    @GetMapping("/submenu")
    public ResponseEntity<CommonResponse<Page<SubMenuResponseDto>>> getAllSubMenus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer menuId,
            @RequestParam(required = false) String subMenuName
    ) {
        log.info("API Call: getAllSubMenus with page: {}, size: {}, menuId: {}, subMenuName: {}",
                page, size, menuId, subMenuName);
        Page<SubMenuResponseDto> result = menuService.getAllSubMenus(page, size, menuId, subMenuName);
        log.info("Fetched all submenus successfully");
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", result));
    }

    @GetMapping("/submenu/{subMenuId}")
    public ResponseEntity<CommonResponse<SubMenuResponseDto>> getSubMenuById(@PathVariable String subMenuId) {
        log.info("API Call: getSubMenuById with subMenuId: {}", subMenuId);
        SubMenuResponseDto dto = menuService.getSubMenuById(subMenuId);

        if (dto == null) {
            log.warn("Sub menu not found: {}", subMenuId);
            return ResponseEntity.status(404).body(new CommonResponse<>(404, "Sub menu not found", null));
        }

        log.info("Fetched sub menu successfully for subMenuId: {}", subMenuId);
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", dto));
    }

    @PutMapping("/submenu/{subMenuId}")
    public ResponseEntity<CommonResponse> updateSubMenu(
            @PathVariable String subMenuId,
            @RequestBody SubMenuRequestDto dto) {

        log.info("API Call: updateSubMenu with subMenuId: {}, request: {}", subMenuId, dto);
        String result = menuService.updateSubMenu(subMenuId, dto);

        if ("Sub menu not found.".equals(result)) {
            log.warn("Sub menu not found: {}", subMenuId);
            return ResponseEntity.status(404).body(new CommonResponse(404, result, null));
        }

        log.info("Sub menu updated successfully: {}", subMenuId);
        return ResponseEntity.ok(new CommonResponse(200, result, null));
    }

    @GetMapping("/dropdown")
    public ResponseEntity<CommonResponse<List<MenuWithSubMenuResponseDto>>> getMenusWithSubMenus() {
        log.info("API Call: getMenusWithSubMenus");
        List<MenuWithSubMenuResponseDto> response = menuService.getMenusWithSubMenus();
        log.info("Fetched menus with submenus successfully");
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", response));
    }
}


