package com.scs.admin.controller;

import com.scs.admin.dto.AssignUserRoleRequestDTO;
import com.scs.admin.dto.CustomResponseDTO;
import com.scs.admin.service.UserRoleAssignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

        try {
            userRoleAssignService.assignUserRole(assignUserRoleRequestDTO);
            log.info("Role assigned successfully to user mobile number: {}", assignUserRoleRequestDTO.getMobileNumber());
            return ResponseEntity.ok(new CustomResponseDTO(200, "User role assigned successfully"));
        } catch (Exception e) {
            log.error("Error assigning role to user mobile number: {}", assignUserRoleRequestDTO.getMobileNumber(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CustomResponseDTO(500, "Failed to assign role"));
        }
    }

    @PostMapping("/assign-sub-role")
    public ResponseEntity<?> assignSubRole(@RequestBody AssignUserRoleRequestDTO assignUserRoleRequestDTO) {
        log.info("Received request to assign sub-role. Role ID: {}, Sub-role ID: {}, User Mobile Number: {}",
                assignUserRoleRequestDTO.getRoleId(),
                assignUserRoleRequestDTO.getSubRoleId(),
                assignUserRoleRequestDTO.getMobileNumber());

        try {
            userRoleAssignService.assignSubRole(assignUserRoleRequestDTO);
            log.info("Sub-role assigned successfully to user mobile number: {}", assignUserRoleRequestDTO.getMobileNumber());
            return ResponseEntity.ok(new CustomResponseDTO(200, "Sub-role assigned successfully"));
        } catch (Exception e) {
            log.error("Error assigning sub-role to user mobile number: {}", assignUserRoleRequestDTO.getMobileNumber(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CustomResponseDTO(500, "Failed to assign sub-role"));
        }
    }
}
