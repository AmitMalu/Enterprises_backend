package com.im.admin.service.impl;

import com.im.admin.entities.Partner;
import com.im.admin.repositories.PartnerRepository;
import com.im.admin.service.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartnerServiceImpl implements PartnerService {

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @Override
    public Partner savePartner(Partner partner) {
        phoneNumberValidationService.validatePhoneNumber(partner.getInfo().getMobile());
        return partnerRepository.save(partner);
    }

    @Override
    public Partner updatePartner(Long id, Partner updatedPartner) {
        return partnerRepository.findById(id)
                .map(existingPartner -> {
                    existingPartner.setParentAccount(updatedPartner.getParentAccount());
                    existingPartner.setInfo(updatedPartner.getInfo());
                    existingPartner.setCompetitor(updatedPartner.getCompetitor());
                    existingPartner.setAddress(updatedPartner.getAddress());
                    existingPartner.setOutletAddress(updatedPartner.getOutletAddress());
                    return partnerRepository.save(existingPartner);
                })
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + id));
    }

    @Override
    public List<Partner> getAllPartners() {
        return partnerRepository.findAll();
    }

    @Override
    public Partner getPartnerById(Long id) {
        return partnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + id));
    }

    @Override
    public void deletePartner(Long id) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + id));
        partnerRepository.delete(partner);
    }
}

