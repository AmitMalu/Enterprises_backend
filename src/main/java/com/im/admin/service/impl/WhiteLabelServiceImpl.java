package com.im.admin.service.impl;

import com.im.admin.entities.WhiteLabelPostpaidAmount;
import com.im.admin.entities.WhiteLabelPrepaidAmount;
import com.im.admin.entities.WhiteLabels;
import com.im.admin.repositories.WhiteLabelPostpaidAmountRepository;
import com.im.admin.repositories.WhiteLabelPrepaidAmountRepository;
import com.im.admin.repositories.WhiteLabelsRepository;
import com.im.admin.service.WhiteLabelService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WhiteLabelServiceImpl implements WhiteLabelService {

    @Autowired
    private  WhiteLabelsRepository whiteLabelRepository;

    @Autowired
    private WhiteLabelPrepaidAmountRepository prepaidRepository;

    @Autowired
    private WhiteLabelPostpaidAmountRepository postpaidRepository;

    @Transactional
    @Override
    public WhiteLabels saveWhiteLabel(WhiteLabels whiteLabel) {
        log.info("Generating sequential WhiteLabel ID...");

        // Generate unique sequential ID
        String abbreviation = whiteLabel.getAbbreviation();
        long count = whiteLabelRepository.count() + 1;
        String whiteLabelId = String.format("%s-WL-%03d", abbreviation, count);
        whiteLabel.setWhiteLabelId(whiteLabelId);

        WhiteLabels savedWhiteLabel = whiteLabelRepository.save(whiteLabel);
        log.info("WhiteLabel saved with ID: {}", whiteLabelId);

        // Save Prepaid Record
        WhiteLabelPrepaidAmount prepaid = new WhiteLabelPrepaidAmount();
        prepaid.setWhiteLabelId(savedWhiteLabel);
        prepaidRepository.save(prepaid);
        log.info("Prepaid amount record created for WhiteLabel: {}", whiteLabelId);

        // Save Postpaid Record
        WhiteLabelPostpaidAmount postpaid = new WhiteLabelPostpaidAmount();
        postpaid.setWhiteLabelId(savedWhiteLabel);
        postpaidRepository.save(postpaid);
        log.info("Postpaid amount record created for WhiteLabel: {}", whiteLabelId);

        return savedWhiteLabel;
    }

    @Transactional
    @Override
    public WhiteLabels updateWhiteLabel(String id, WhiteLabels whiteLabel) {
        log.info("Updating WhiteLabel: {}", id);
        WhiteLabels existing = whiteLabelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WhiteLabel not found with ID: " + id));

        existing.setTitle(whiteLabel.getTitle());
        existing.setMobileNo(whiteLabel.getMobileNo());
        existing.setAddress(whiteLabel.getAddress());
        existing.setDomainURL(whiteLabel.getDomainURL());
        existing.setIsServiceAllow(whiteLabel.getIsServiceAllow());
        existing.setEditedBy(whiteLabel.getEditedBy());
        existing.setEditedOn(LocalDateTime.now());

        return whiteLabelRepository.save(existing);
    }

    @Override
    public List<WhiteLabels> getAllWhiteLabels() {
        log.info("Fetching all WhiteLabels...");
        return whiteLabelRepository.findAll();
    }

    @Override
    public WhiteLabels getWhiteLabelById(String id) {
        log.info("Fetching WhiteLabel by ID: {}", id);
        return whiteLabelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WhiteLabel not found with ID: " + id));
    }

    @Transactional
    @Override
    public void deleteWhiteLabel(String id) {
        log.info("Deleting WhiteLabel: {}", id);
        WhiteLabels whiteLabel = whiteLabelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WhiteLabel not found with ID: " + id));

        prepaidRepository.deleteByWhiteLabelId(whiteLabel);
        postpaidRepository.deleteByWhiteLabelId(whiteLabel);
        whiteLabelRepository.delete(whiteLabel);
        log.info("WhiteLabel deleted successfully: {}", id);
    }
}

