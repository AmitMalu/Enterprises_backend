package com.im.admin.service.impl;

import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Override
    public String updateUserBlockStatus(String hierarchyId, boolean block) {
        List<UserDetails> userList = userDetailsRepository.findByHierarchyId(hierarchyId);

        if (userList.isEmpty()) {
            throw new RuntimeException("User with hierarchyId " + hierarchyId + " not found.");
        }

        for (UserDetails user : userList) {
            user.setIsBlocked(block);
            user.setEditedDate(LocalDate.now());
            user.setEditedTime(LocalTime.now());
//            user.setEditedBy("Admin"); // You can set dynamic value as per context
        }

        userDetailsRepository.saveAll(userList);

        return block ? "User blocked successfully." : "User unblocked successfully.";
    }
}
