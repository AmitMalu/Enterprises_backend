package com.im.admin.controller;

import com.im.admin.dto.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @PostMapping
    public ResponseEntity<CommonResponse> addMenu(@RequestBody MenuRequestDto menuRequestDto) {
        String result = menuService.addMenu(menuRequestDto);

        if ("Menu already exists.".equals(result)) {
            return ResponseEntity
                    .status(409)
                    .body(new CommonResponse(409, result, null));
        }

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
        Page<MenuResponseDto> result = menuService.getAllMenus(page, size, menuId, menuName);
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", result));
    }

    @GetMapping("/{menuId}")
    public ResponseEntity<CommonResponse<MenuResponseDto>> getMenuById(@PathVariable String menuId) {
        MenuResponseDto dto = menuService.getMenuById(menuId);

        if (dto == null) {
            return ResponseEntity.status(404).body(new CommonResponse<>(404, "Menu not found", null));
        }

        return ResponseEntity.ok(new CommonResponse<>(200, "Success", dto));
    }

    @PutMapping("/{menuId}")
    public ResponseEntity<CommonResponse> updateMenu(
            @PathVariable String menuId,
            @RequestBody MenuRequestDto requestDto) {

        String result = menuService.updateMenu(menuId, requestDto);

        if ("Menu not found.".equals(result)) {
            return ResponseEntity.status(404).body(new CommonResponse(404, result, null));
        }

        return ResponseEntity.ok(new CommonResponse(200, result, null));
    }

    @PostMapping("/submenu")
    public ResponseEntity<CommonResponse> addSubMenu(@RequestBody SubMenuRequestDto dto) {
        String result = menuService.addSubMenu(dto);

        if ("Sub menu already exists.".equals(result)) {
            return ResponseEntity.status(409).body(new CommonResponse(409, result, null));
        }

        return ResponseEntity.ok(new CommonResponse(201, result, null));
    }


    @GetMapping("/submenu")
    public ResponseEntity<CommonResponse<Page<SubMenuResponseDto>>> getAllSubMenus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer menuId,
            @RequestParam(required = false) String subMenuName
    ) {
        Page<SubMenuResponseDto> result = menuService.getAllSubMenus(page, size, menuId, subMenuName);
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", result));
    }

    @GetMapping("/submenu/{subMenuId}")
    public ResponseEntity<CommonResponse<SubMenuResponseDto>> getSubMenuById(@PathVariable String subMenuId) {
        SubMenuResponseDto dto = menuService.getSubMenuById(subMenuId);
        if (dto == null) {
            return ResponseEntity.status(404).body(new CommonResponse<>(404, "Sub menu not found", null));
        }
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", dto));
    }

    @PutMapping("/submenu/{subMenuId}")
    public ResponseEntity<CommonResponse> updateSubMenu(
            @PathVariable String subMenuId,
            @RequestBody SubMenuRequestDto dto) {

        String result = menuService.updateSubMenu(subMenuId, dto);

        if ("Sub menu not found.".equals(result)) {
            return ResponseEntity.status(404).body(new CommonResponse(404, result, null));
        }

        return ResponseEntity.ok(new CommonResponse(200, result, null));
    }

    @GetMapping("/dropdown")
    public ResponseEntity<CommonResponse<List<MenuWithSubMenuResponseDto>>> getMenusWithSubMenus() {
        List<MenuWithSubMenuResponseDto> response = menuService.getMenusWithSubMenus();
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", response));
    }


}


