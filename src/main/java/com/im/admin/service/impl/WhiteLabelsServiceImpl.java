package com.im.admin.service.impl;

import com.im.admin.entities.WhiteLabels;
import com.im.admin.repositories.WhiteLabelsRepository;
import com.im.admin.service.WhiteLabelsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;

@Service
public class WhiteLabelsServiceImpl implements WhiteLabelsService {

    @Autowired
    private WhiteLabelsRepository whiteLabelsRepository;

    @Override
    public WhiteLabels saveWhiteLabel(WhiteLabels whiteLabels) {
        return whiteLabelsRepository.save(whiteLabels);
    }

    @Override
    public WhiteLabels updateWhiteLabel(Long id, WhiteLabels whiteLabels) {
        return whiteLabelsRepository.findById(id).map(existing -> {
            existing.setImAccountMemberID(whiteLabels.getImAccountMemberID());
            existing.setTitle(whiteLabels.getTitle());
            existing.setAddress(whiteLabels.getAddress());
            existing.setDomainURL(whiteLabels.getDomainURL());
            existing.setTermsAndConditionURL(whiteLabels.getTermsAndConditionURL());
            existing.setPrivacyPolicyURL(whiteLabels.getPrivacyPolicyURL());
            existing.setRefundAndCancellationURL(whiteLabels.getRefundAndCancellationURL());
            existing.setServiceAllow(whiteLabels.getServiceAllow());
            existing.setAllowPrepaidCards(whiteLabels.getAllowPrepaidCards());
            existing.setPrepaidCardName(whiteLabels.getPrepaidCardName());
            existing.setAllowPostpaidCards(whiteLabels.getAllowPostpaidCards());
            existing.setPostpaidCardName(whiteLabels.getPostpaidCardName());
            existing.setProducts(whiteLabels.getProducts());
            existing.setRedirect(whiteLabels.getRedirect());
            existing.setAppID(whiteLabels.getAppID());
            existing.setCaptchaSiteKey(whiteLabels.getCaptchaSiteKey());
            existing.setCaptchaSecretKey(whiteLabels.getCaptchaSecretKey());
            existing.setRemarks(whiteLabels.getRemarks());
            existing.setTheme(whiteLabels.getTheme());
            return whiteLabelsRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("WhiteLabel not found with ID: " + id));
    }

    @Override
    public WhiteLabels getWhiteLabelById(Long id) {
        return whiteLabelsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WhiteLabel not found with ID: " + id));
    }

    @Override
    public Page<WhiteLabels> getAllWhiteLabels(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return whiteLabelsRepository.findAll(pageable);
    }


    @Override
    public void deleteWhiteLabel(Long id) {
        if (!whiteLabelsRepository.existsById(id)) {
            throw new RuntimeException("WhiteLabel not found with ID: " + id);
        }
        whiteLabelsRepository.deleteById(id);
    }
}

