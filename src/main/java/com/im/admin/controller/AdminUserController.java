package com.im.admin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.UserRequestDTO;
import com.im.admin.dto.UserResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.UnifiedUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Single controller for AdminUser and WhiteLabelUser.
 *
 * The frontend sends the exact same request it always did — no new fields.
 * The backend derives which table to use automatically:
 *
 *   CREATE / GET-ALL → from parentAccount segment[1]
 *     "MAY-AD-001" → admin_users
 *     "MAY-WL-001" → white_label_users
 *
 *   GET-BY-ID / UPDATE → from the id itself
 *     "MAY-AU-0001"  → admin_users
 *     "MAY-WAU-0001" → white_label_users
 *
 * Endpoints (unchanged from original admin-user URLs):
 *   POST   /api/admin-user/create
 *   GET    /api/admin-user?parentAccount=MAY-AD-001
 *   GET    /api/admin-user/{id}
 *   PUT    /api/admin-user/{id}
 *
 * @author SHUBHAM KHOPADE
 */
@Slf4j
@RestController
@RequestMapping("/api/admin-user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UnifiedUserService unifiedUserService;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/create")
    public ResponseEntity<CommonResponse<UserResponseDTO>> createUser(
            @RequestBody UserRequestDTO dto) throws JsonProcessingException {

        log.info("createUser — parentAccount={}", dto.getParentAccount());

        UserResponseDTO saved = unifiedUserService.createUser(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonResponse.created(saved, "User created successfully")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL  (paginated + filtered)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * parentAccount (required) is used to derive which table to query.
     * All other params are optional filters.
     */
    @GetMapping
    public ResponseEntity<CommonResponse<Page<UserResponseDTO>>> getAllUsers(
            @RequestParam String parentAccount,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String emailId,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dob,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate createdOn,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate lastEditedOn,
            @RequestParam(required = false) String editedBy
    ) throws JsonProcessingException {

        log.info("getAllUsers — parentAccount={} page={} size={}", parentAccount, page, size);

        Page<UserResponseDTO> result = unifiedUserService.getAllUsers(
                page, size, parentAccount,
                id, name, mobileNumber, username, emailId,
                status, dob, createdOn, createdBy, lastEditedOn, editedBy
        );

        return ResponseEntity.ok(
                CommonResponse.success(result, "Users fetched successfully")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<UserResponseDTO>> getUserById(
            @PathVariable String id) throws JsonProcessingException {

        log.info("getUserById — id={}", id);

        UserResponseDTO dto = unifiedUserService.getUserById(id);

        return ResponseEntity.ok(
                CommonResponse.success(dto, "User fetched successfully")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<UserResponseDTO>> updateUser(
            @PathVariable String id,
            @RequestBody UserRequestDTO dto) throws JsonProcessingException {

        log.info("updateUser — id={}", id);

        UserResponseDTO updated = unifiedUserService.updateUser(id, dto);

        return ResponseEntity.ok(
                CommonResponse.success(updated, "User updated successfully")
        );
    }
}
