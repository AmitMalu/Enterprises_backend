package com.im.admin.controller;

import com.im.admin.dto.AssignUserRoleRequestDTO;
import com.im.admin.dto.CustomResponseDTO;
import com.im.admin.service.UserRoleAssignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserRoleAssignController {
    @Autowired
    private UserRoleAssignService userRoleAssignService;

    @PostMapping("/assign-role")
    public ResponseEntity<CustomResponseDTO> assignRoleToUser(@RequestBody AssignUserRoleRequestDTO assignUserRoleRequestDTO) {
        log.info("Received request to assign role: {}", assignUserRoleRequestDTO);
        System.out.println(assignUserRoleRequestDTO.getMobileNumber());
        userRoleAssignService.assignUserRole(assignUserRoleRequestDTO);
        return ResponseEntity.ok(new CustomResponseDTO(200, "User role assigned successfully"));
    }

    @PostMapping("/assign-sub-role")
    public ResponseEntity<?> assignSubRole(@RequestBody AssignUserRoleRequestDTO assignUserRoleRequestDTO) {
        log.info("Assigning role: {} and sub-role: {}", assignUserRoleRequestDTO.getRoleId(), assignUserRoleRequestDTO.getSubRoleId());

        userRoleAssignService.assignSubRole(assignUserRoleRequestDTO);

        return ResponseEntity.ok(new CustomResponseDTO(200, "Sub-role assigned successfully"));
    }
}
