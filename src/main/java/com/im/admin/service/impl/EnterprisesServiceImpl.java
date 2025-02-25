package com.im.admin.service.impl;

import com.im.admin.entities.Enterprises;
import com.im.admin.repositories.EnterprisesRepository;
import com.im.admin.service.EnterprisesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Slf4j
@Service
public class EnterprisesServiceImpl implements EnterprisesService {

    @Autowired
    private EnterprisesRepository enterpriseRepository;

    @Override
    public Enterprises saveEnterprise(Enterprises enterprise) {
        log.info("Saving new enterprise: {}", enterprise.getCompanyName());
        return enterpriseRepository.save(enterprise);
    }

    @Override
    public Enterprises updateEnterprise(Long id, Enterprises updatedEnterprise) {
        log.info("Updating enterprise with ID: {}", id);
        return enterpriseRepository.findById(id)
                .map(existingEnterprise -> {
                    existingEnterprise.setCompanyName(updatedEnterprise.getCompanyName());
                    existingEnterprise.setCompanyType(updatedEnterprise.getCompanyType());
                    existingEnterprise.setRegisteredAddress(updatedEnterprise.getRegisteredAddress());
                    existingEnterprise.setBusinessDesc(updatedEnterprise.getBusinessDesc());
                    existingEnterprise.setWebsiteURL(updatedEnterprise.getWebsiteURL());
                    existingEnterprise.setPanCardNumber(updatedEnterprise.getPanCardNumber());
                    existingEnterprise.setGSTNumber(updatedEnterprise.getGSTNumber());
                    existingEnterprise.setBankAccountDetails(updatedEnterprise.getBankAccountDetails());
                    existingEnterprise.setFinancialStatements(updatedEnterprise.getFinancialStatements());
                    existingEnterprise.setDirectorName(updatedEnterprise.getDirectorName());
                    existingEnterprise.setDirectorEmailId(updatedEnterprise.getDirectorEmailId());
                    existingEnterprise.setDirectorMobileNumber(updatedEnterprise.getDirectorMobileNumber());
                    existingEnterprise.setAuthorizedSignatoryName(updatedEnterprise.getAuthorizedSignatoryName());
                    existingEnterprise.setAuthorizedSignatoryEmail(updatedEnterprise.getAuthorizedSignatoryEmail());
                    return enterpriseRepository.save(existingEnterprise);
                })
                .orElseThrow(() -> {
                    log.error("Enterprise with ID {} not found", id);
                    return new RuntimeException("Enterprise not found");
                });
    }

    @Override
    public void deleteEnterprise(Long id) {
        log.info("Deleting enterprise with ID: {}", id);
        if (enterpriseRepository.existsById(id)) {
            enterpriseRepository.deleteById(id);
            log.info("Enterprise with ID {} deleted successfully", id);
        } else {
            log.error("Enterprise with ID {} not found", id);
            throw new RuntimeException("Enterprise not found");
        }
    }

    @Override
    public Page<Enterprises> getAllEnterprises(Pageable pageable) {
        log.info("Fetching all enterprises with pagination");
        return enterpriseRepository.findAll(pageable);
    }

    @Override
    public Optional<Enterprises> getEnterpriseById(Long id) {
        log.info("Fetching enterprise with ID: {}", id);
        return enterpriseRepository.findById(id);
    }
}


