package com.im.admin.service.impl;

import com.im.admin.entities.Enterprises;
import com.im.admin.entities.EnterprisesPostpaidAmount;
import com.im.admin.entities.EnterprisesPrepaidAmount;
import com.im.admin.repositories.EnterprisesPostpaidAmountRepository;
import com.im.admin.repositories.EnterprisesPrepaidAmountRepository;
import com.im.admin.repositories.EnterprisesRepository;
import com.im.admin.service.EnterprisesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnterprisesServiceImpl implements EnterprisesService {

    @Autowired
    private EnterprisesRepository enterpriseRepository;

    @Autowired
    private EnterprisesPrepaidAmountRepository prepaidRepository;

    @Autowired
    private EnterprisesPostpaidAmountRepository postpaidRepository;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @Transactional
    @Override
    public Enterprises saveEnterprise(Enterprises enterprise) {

        phoneNumberValidationService.validatePhoneNumber(enterprise.getMobileNo());

        String abbreviation = enterprise.getAbbreviation();
        long count = enterpriseRepository.count() + 1;
        String enterpriseId = String.format("%s-ET-%03d", abbreviation, count);
        enterprise.setEnterpriseId(enterpriseId);

        Enterprises savedEnterprise = enterpriseRepository.save(enterprise);

        EnterprisesPrepaidAmount prepaid = new EnterprisesPrepaidAmount();
        prepaid.setEnterprise(savedEnterprise);
        prepaidRepository.save(prepaid);

        EnterprisesPostpaidAmount postpaid = new EnterprisesPostpaidAmount();
        postpaid.setEnterprise(savedEnterprise);
        postpaidRepository.save(postpaid);

        return savedEnterprise;
    }

    @Transactional
    @Override
    public Enterprises updateEnterprise(String enterpriseId, Enterprises enterpriseDetails) {
        Enterprises existingEnterprise = enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new RuntimeException("Enterprise not found with ID: " + enterpriseId));

        existingEnterprise.setCompanyName(enterpriseDetails.getCompanyName());
        existingEnterprise.setMobileNo(enterpriseDetails.getMobileNo());
        existingEnterprise.setCompanyType(enterpriseDetails.getCompanyType());
        existingEnterprise.setRegisteredAddress(enterpriseDetails.getRegisteredAddress());
        existingEnterprise.setBusinessDesc(enterpriseDetails.getBusinessDesc());
        existingEnterprise.setWebsiteURL(enterpriseDetails.getWebsiteURL());
        existingEnterprise.setPanCardNumber(enterpriseDetails.getPanCardNumber());
        existingEnterprise.setGSTNumber(enterpriseDetails.getGSTNumber());
        existingEnterprise.setBankAccountDetails(enterpriseDetails.getBankAccountDetails());
        existingEnterprise.setFinancialStatements(enterpriseDetails.getFinancialStatements());

        return enterpriseRepository.save(existingEnterprise);
    }

    @Override
    public List<Enterprises> getAllEnterprises() {
        return enterpriseRepository.findAll();
    }

    @Override
    public Enterprises getEnterpriseById(String enterpriseId) {
        return enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new RuntimeException("Enterprise not found with ID: " + enterpriseId));
    }

    @Transactional
    @Override
    public void deleteEnterprise(String enterpriseId) {
        Enterprises enterprise = enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new RuntimeException("Enterprise not found with ID: " + enterpriseId));

        prepaidRepository.deleteByEnterprise(enterprise);
        postpaidRepository.deleteByEnterprise(enterprise);
        enterpriseRepository.delete(enterprise);
    }
}



