package com.im.admin.service.impl;

import com.im.admin.dto.AssignUserRoleRequestDTO;
import com.im.admin.entities.ImSubRole;
import com.im.admin.entities.UserDetails;
import com.im.admin.exception.UserNotFoundException;
import com.im.admin.repositories.ImSubRoleRepository;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.service.UserRoleAssignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class UserRoleAssignServiceImpl implements UserRoleAssignService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private ImSubRoleRepository imSubRoleRepository;

    @Override
    public void assignUserRole(AssignUserRoleRequestDTO assignUserRoleRequestDTO) {
        log.info("Assigning role [{}] to mobile number [{}]",
                assignUserRoleRequestDTO.getRoleId(),
                assignUserRoleRequestDTO.getMobileNumber());

        Optional<UserDetails> userOptional = userDetailsRepository.findByMobileNumber(assignUserRoleRequestDTO.getMobileNumber());

        if (userOptional.isEmpty()) {
            log.warn("Mobile number not registered: {}", assignUserRoleRequestDTO.getMobileNumber());
            throw new UserNotFoundException("Mobile number not registered");
        }

        UserDetails userDetails = userOptional.get();
        userDetails.setRole_id(assignUserRoleRequestDTO.getRoleId()); // assuming this is the correct field
        userDetailsRepository.save(userDetails);

        log.info("User role assigned successfully to mobile number [{}]", assignUserRoleRequestDTO.getMobileNumber());
    }

    @Override
    public void assignSubRole(AssignUserRoleRequestDTO assignUserRoleRequestDTO) {
        log.info("Assigning sub-role to mobile: {}", assignUserRoleRequestDTO.getMobileNumber());

        // Check if user is registered
        Optional<UserDetails> user = userDetailsRepository.findByMobileNumber(assignUserRoleRequestDTO.getMobileNumber());

        if (user.isEmpty()) {
            log.error("Mobile number {} is not registered", assignUserRoleRequestDTO.getMobileNumber());
            throw new UserNotFoundException("Mobile number not registered");
        }

        log.info("User found for mobile: {}. Proceeding to assign or update sub-role.", assignUserRoleRequestDTO.getMobileNumber());

        // Check if sub-role already assigned for this mobile
        ImSubRole existing = imSubRoleRepository.findByMobileNumber(assignUserRoleRequestDTO.getMobileNumber());

        if (existing != null) {
            // If sub-role exists, update it
            log.info("Updating sub-role for mobile: {}", assignUserRoleRequestDTO.getMobileNumber());
            existing.setSubRoleId(assignUserRoleRequestDTO.getSubRoleId());
            imSubRoleRepository.save(existing); // Save the updated sub-role
            log.info("Sub-role updated successfully for mobile: {}", assignUserRoleRequestDTO.getMobileNumber());
        } else {
            // If sub-role doesn't exist, create a new one
            log.info("Assigning new sub-role for mobile: {}", assignUserRoleRequestDTO.getMobileNumber());
            ImSubRole newEntity = new ImSubRole();
            newEntity.setSubRoleId(assignUserRoleRequestDTO.getSubRoleId());
            newEntity.setMobileNumber(assignUserRoleRequestDTO.getMobileNumber());
            imSubRoleRepository.save(newEntity); // Save new sub-role
            log.info("Sub-role saved successfully for mobile: {}", assignUserRoleRequestDTO.getMobileNumber());
        }
    }
    }



