package com.scs.admin.repositories;

import org.springframework.stereotype.Repository;

@Repository
public interface CommonPhoneNumberRepository {

    String findExistingHierarchyByPhoneNumber(String phoneNumber);

}


