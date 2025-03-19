package com.im.admin.service;

import com.im.admin.entities.Enterprises;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface EnterprisesService {

    Enterprises saveEnterprise(Enterprises enterprise);

    Enterprises updateEnterprise(String id, Enterprises enterprise);

    List<Enterprises> getAllEnterprises();

    Enterprises getEnterpriseById(String id);

    void deleteEnterprise(String id);
}



