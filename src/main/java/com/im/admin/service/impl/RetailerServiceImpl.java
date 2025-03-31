package com.im.admin.service.impl;

import com.im.admin.dto.RetailerDTO;
import com.im.admin.entities.Retailer;
import com.im.admin.entities.RetailerPostpaidAmount;
import com.im.admin.entities.RetailerPrepaidAmount;
import com.im.admin.repositories.RetailerPostpaidAmountRepository;
import com.im.admin.repositories.RetailerPrepaidAmountRepository;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.service.RetailerService;
import com.im.admin.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RetailerServiceImpl implements RetailerService {

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private RetailerPrepaidAmountRepository prepaidAmountRepository;

    @Autowired
    private RetailerPostpaidAmountRepository postpaidAmountRepository;

    @Autowired
    private IdGenerator idGenerator;


    @Transactional
    @Override
    public Retailer createRetailer(Retailer retailer) {
        log.info("Creating new retailer: {}", retailer.getName());

        retailer.setRetailerId(idGenerator.generateHierarchyEntityId("Retailer", retailer.getAbbreviation()));

        Retailer savedRetailer = retailerRepository.save(retailer);
        log.info("Retailer created successfully with ID: {}", savedRetailer.getRetailerId());

        RetailerPrepaidAmount prepaidAmount = new RetailerPrepaidAmount();
        prepaidAmount.setRetailerId(savedRetailer);
        prepaidAmountRepository.save(prepaidAmount);
        log.info("Prepaid amount record created for Retailer ID: {}", savedRetailer.getRetailerId());

        RetailerPostpaidAmount postpaidAmount = new RetailerPostpaidAmount();
        postpaidAmount.setRetailerId(savedRetailer);
        postpaidAmountRepository.save(postpaidAmount);
        log.info("Postpaid amount record created for Retailer ID: {}", savedRetailer.getRetailerId());

        return savedRetailer;
    }

    @Transactional
    @Override
    public Retailer updateRetailer(String retailerId, Retailer retailerDetails) {
        log.info("Updating retailer with ID: {}", retailerId);

        Retailer retailer = retailerRepository.findById(retailerId)
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        retailer.setParentAccount(retailerDetails.getParentAccount());
        retailer.setTitle(retailerDetails.getTitle());
        retailer.setName(retailerDetails.getName());
        retailer.setMobile(retailerDetails.getMobile());
        retailer.setEmail(retailerDetails.getEmail());
        retailer.setDob(retailerDetails.getDob());
        retailer.setOutletName(retailerDetails.getOutletName());
        retailer.setCompanyType(retailerDetails.getCompanyType());
        retailer.setMccCategory(retailerDetails.getMccCategory());
        retailer.setOrmsId(retailerDetails.getOrmsId());
        retailer.setCompetitor(retailerDetails.getCompetitor());
        retailer.setAddress(retailerDetails.getAddress());
        retailer.setOutletAddress(retailerDetails.getOutletAddress());
        retailer.setDistrict(retailerDetails.getDistrict());

        Retailer updatedRetailer = retailerRepository.save(retailer);
        log.info("Retailer updated successfully with ID: {}", updatedRetailer.getRetailerId());
        return updatedRetailer;
    }

    @Transactional
    @Override
    public void deleteRetailer(String retailerId) {
        log.info("Deleting retailer with ID: {}", retailerId);

        Retailer retailer = retailerRepository.findById(retailerId)
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        // Delete associated Prepaid Amount record
        prepaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                .ifPresent(prepaidAmount -> {
                    prepaidAmountRepository.delete(prepaidAmount);
                    log.info("Deleted prepaid amount record for Retailer ID: {}", retailerId);
                });

        // Delete associated Postpaid Amount record
        postpaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                .ifPresent(postpaidAmount -> {
                    postpaidAmountRepository.delete(postpaidAmount);
                    log.info("Deleted postpaid amount record for Retailer ID: {}", retailerId);
                });

        // Delete retailer
        retailerRepository.delete(retailer);
        log.info("Retailer deleted successfully with ID: {}", retailerId);
    }


    @Override
    public List<Retailer> getAllRetailers() {
        log.info("Fetching all retailers");
        return retailerRepository.findAll();
    }

    @Override
    public Optional<Retailer> getRetailerById(String retailerId) {
        log.info("Fetching retailer by ID: {}", retailerId);
        return retailerRepository.findById(retailerId);
    }
}

