package com.im.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.UserRequestDTO;
import com.im.admin.dto.UserResponseDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

/**
 * Unified service for AdminUser and WhiteLabelUser.
 *
 * The userType ("AD" / "WL") is never sent by the frontend.
 * It is derived internally:
 *   - create / getAll : extracted from parentAccount.split("-")[1]
 *   - getById / update: extracted from the record's id prefix  (AU → AD, WAU → WL)
 *
 * @author SHUBHAM KHOPADE
 */
public interface UnifiedUserService {

    /** parentAccount in dto drives which table is used */
    UserResponseDTO createUser(UserRequestDTO dto) throws JsonProcessingException;

    /**
     * parentAccount is passed so the service can derive which table to query.
     * e.g. "MAY-WL-001" → WL table,  "MAY-AD-001" → AD table
     */
    Page<UserResponseDTO> getAllUsers(
            int page,
            int size,
            String parentAccount,
            String id,
            String name,
            String mobileNumber,
            String username,
            String emailId,
            Boolean status,
            LocalDate dob,
            LocalDate createdOn,
            String createdBy,
            LocalDate lastEditedOn,
            String editedBy
    ) throws JsonProcessingException;

    /**
     * id contains the hierarchy code prefix that tells us the table:
     *   MAY-AU-0001  → AU prefix → admin_users
     *   MAY-WAU-0001 → WAU prefix → white_label_users
     */
    UserResponseDTO getUserById(String id) throws JsonProcessingException;

    /**
     * Same as getUserById — id prefix drives which table to update.
     */
    UserResponseDTO updateUser(String id, UserRequestDTO dto) throws JsonProcessingException;
}
