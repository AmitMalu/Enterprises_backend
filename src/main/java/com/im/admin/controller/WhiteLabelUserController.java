package com.im.admin.controller;

/**
 * @author SHUBHAM KHOPADE
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.AdminUserRequestDTO;
import com.im.admin.dto.WhiteLabelUserRequestDTO;
import com.im.admin.dto.WhiteLabelUserResponseDTO;
import com.im.admin.entities.AdminUser;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AdminUserService;
import com.im.admin.service.WhiteLabelUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/whitelabel-user")
@RequiredArgsConstructor
public class WhiteLabelUserController {

    private final WhiteLabelUserService whiteLabelUserService;

    @PostMapping("/create")
    public ResponseEntity<CommonResponse<WhiteLabelUserResponseDTO>> createUser(
            @RequestBody WhiteLabelUserRequestDTO dto) throws JsonProcessingException {

        log.info("Creating white label user: {}", dto);

        WhiteLabelUserResponseDTO responseDto =
                whiteLabelUserService.createWhiteLabelUser(dto);

        CommonResponse<WhiteLabelUserResponseDTO> response =
                new CommonResponse<>();

        response.setStatusCode(HttpStatus.CREATED.value());
        response.setMessage("White Label User created successfully");
        response.setData(responseDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<WhiteLabelUserResponseDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String emailId,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dob,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate createdOn,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate lastEditedOn,
            @RequestParam(required = false) String editedBy
    )  throws JsonProcessingException {

        Page<WhiteLabelUserResponseDTO> result =
                whiteLabelUserService.getAllWhiteLabelUsers(
                        page,
                        size,
                        id,
                        name,
                        mobileNumber,
                        username,
                        emailId,
                        status,
                        dob,
                        createdOn,
                        createdBy,
                        lastEditedOn,
                        editedBy
                );

        CommonResponse<Page<WhiteLabelUserResponseDTO>> response =
                new CommonResponse<>();

        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("White Label Users fetched successfully");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<WhiteLabelUserResponseDTO>> getById(
            @PathVariable String id) throws JsonProcessingException {

        WhiteLabelUserResponseDTO dto =
                whiteLabelUserService.getWhiteLabelUserById(id);

        CommonResponse<WhiteLabelUserResponseDTO> response =
                new CommonResponse<>();

        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("White Label User fetched successfully");
        response.setData(dto);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<WhiteLabelUserResponseDTO>> update(
            @PathVariable String id,
            @RequestBody WhiteLabelUserRequestDTO dto) throws JsonProcessingException {

        WhiteLabelUserResponseDTO updated =
                whiteLabelUserService.updateWhiteLabelUser(id, dto);

        CommonResponse<WhiteLabelUserResponseDTO> response =
                new CommonResponse<>();

        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("White Label User updated successfully");
        response.setData(updated);

        return ResponseEntity.ok(response);
    }
}

