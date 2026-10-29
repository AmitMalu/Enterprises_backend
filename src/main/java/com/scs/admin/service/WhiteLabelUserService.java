package com.scs.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scs.admin.dto.WhiteLabelUserRequestDTO;
import com.scs.admin.dto.WhiteLabelUserResponseDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

/**
 * @author SHUBHAM KHOPADE
 */
public interface WhiteLabelUserService {

    WhiteLabelUserResponseDTO createWhiteLabelUser(
            WhiteLabelUserRequestDTO requestDTO
    ) throws JsonProcessingException;

    Page<WhiteLabelUserResponseDTO> getAllWhiteLabelUsers(
            int page,
            int size,
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

    WhiteLabelUserResponseDTO updateWhiteLabelUser(
            String id,
            WhiteLabelUserRequestDTO requestDTO
    ) throws JsonProcessingException;

    WhiteLabelUserResponseDTO getWhiteLabelUserById(
            String id
    ) throws JsonProcessingException;
}
