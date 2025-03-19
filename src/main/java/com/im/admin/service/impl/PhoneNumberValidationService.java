package com.im.admin.service.impl;

import com.im.admin.repositories.CommonPhoneNumberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PhoneNumberValidationService {

    @Autowired
    private CommonPhoneNumberRepository commonPhoneNumberRepository;

    public void validatePhoneNumber(String phoneNumber) {
        String existingHierarchy = commonPhoneNumberRepository.findExistingHierarchyByPhoneNumber(phoneNumber);

        if (existingHierarchy != null) {
            log.warn("Phone number {} already exists in {}.", phoneNumber, existingHierarchy);
            throw new IllegalArgumentException("Phone number already exists in " + existingHierarchy + ".");
        }
    }
}


