package com.im.admin.service.impl;

import com.im.admin.entities.Partner;
import com.im.admin.entities.PartnerPostpaidAmount;
import com.im.admin.entities.PartnerPrepaidAmount;
import com.im.admin.repositories.PartnerPostpaidAmountRepository;
import com.im.admin.repositories.PartnerPrepaidAmountRepository;
import com.im.admin.repositories.PartnerRepository;
import com.im.admin.service.PartnerService;
import com.im.admin.util.IdGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class PartnerServiceImpl implements PartnerService {

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private PartnerPrepaidAmountRepository partnerPrepaidAmountRepository;

    @Autowired
    private PartnerPostpaidAmountRepository partnerPostpaidAmountRepository;

    @Autowired
    private IdGenerator idGenerator; // Assuming you have an ID generator utility

    @Override
    @Transactional
    public Partner savePartner(Partner partner) {
        try {
            log.info("Saving new Partner: {}", partner.getName());

            String abbreviation = partner.getAbbreviation();
            partner.setPartnerId(idGenerator.generateHierarchyEntityId("Partner", abbreviation));

            Partner savedPartner = partnerRepository.save(partner);

            PartnerPrepaidAmount prepaidAmount = new PartnerPrepaidAmount();
            prepaidAmount.setPartnerId(savedPartner);
            partnerPrepaidAmountRepository.save(prepaidAmount);

            PartnerPostpaidAmount postpaidAmount = new PartnerPostpaidAmount();
            postpaidAmount.setPartnerId(savedPartner);
            partnerPostpaidAmountRepository.save(postpaidAmount);

            log.info("Successfully saved Partner: {}", savedPartner.getPartnerId());
            return savedPartner;
        } catch (Exception e) {
            log.error("Failed to save Partner: {}", e.getMessage());
            throw new RuntimeException("Failed to save Partner", e);
        }
    }

    @Override
    public Partner updatePartner(String partnerId, Partner updatedPartner) {
        log.info("Updating Partner with ID: {}", partnerId);
        return partnerRepository.findById(partnerId).map(existingPartner -> {
            existingPartner.setName(updatedPartner.getName());
            existingPartner.setTitle(updatedPartner.getTitle());
            existingPartner.setMobile(updatedPartner.getMobile());
            existingPartner.setEmail(updatedPartner.getEmail());
            existingPartner.setOutletName(updatedPartner.getOutletName());
            existingPartner.setCompanyType(updatedPartner.getCompanyType());
            existingPartner.setCompetitor(updatedPartner.getCompetitor());
            existingPartner.setAddress(updatedPartner.getAddress());
            existingPartner.setOutletAddress(updatedPartner.getOutletAddress());

            Partner saved = partnerRepository.save(existingPartner);
            log.info("Successfully updated Partner: {}", partnerId);
            return saved;
        }).orElseThrow(() -> new RuntimeException("Partner not found"));
    }

    @Override
    public List<Partner> getAllPartners() {
        log.info("Fetching all Partners");
        return partnerRepository.findAll();
    }

    @Override
    public Partner getPartnerById(String partnerId) {
        log.info("Fetching Partner with ID: {}", partnerId);
        return partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found"));
    }

    @Override
    public void deletePartner(String partnerId) {
        log.info("Deleting Partner with ID: {}", partnerId);

        Partner partner = partnerRepository.findById(partnerId).
                orElseThrow(() -> new EntityNotFoundException("Partner Not Found " + partnerId));

        partnerPostpaidAmountRepository.deleteByPartnerId(partner);
        partnerPrepaidAmountRepository.deleteByPartnerId(partner);
        partnerRepository.deleteById(partnerId);
        log.info("Successfully deleted Partner with ID: {}", partnerId);
    }
}


