package com.scs.admin.repositories;

import com.scs.admin.entities.EmailTemplateMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplateMaster, Long> {

    Optional<EmailTemplateMaster> findByType(String type);

}
