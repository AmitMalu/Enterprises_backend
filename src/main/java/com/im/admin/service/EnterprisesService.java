package com.im.admin.service;

import com.im.admin.entities.Enterprises;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface EnterprisesService {

    Enterprises saveEnterprise(Enterprises enterprise);

    Enterprises updateEnterprise(Long id, Enterprises enterprise);

    void deleteEnterprise(Long id);

    Page<Enterprises> getAllEnterprises(Pageable pageable);

    Optional<Enterprises> getEnterpriseById(Long id);
}


