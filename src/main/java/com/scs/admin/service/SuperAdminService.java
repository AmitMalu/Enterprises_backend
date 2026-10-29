package com.scs.admin.service;


import com.scs.admin.entities.SuperAdmin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface SuperAdminService {

    SuperAdmin saveSuperAdmin(SuperAdmin superAdmin);

    SuperAdmin getSuperAdminById(String id);

    List<SuperAdmin> getAllSuperAdmins(int page, int size);

    SuperAdmin updateSuperAdmin(String id, SuperAdmin updatedSuperAdmin);

    void deleteSuperAdmin(String id);

}

