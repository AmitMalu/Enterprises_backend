package com.im.admin.service;

import com.im.admin.entities.WhiteLabels;
import org.springframework.data.domain.Page;

public interface WhiteLabelsService {
    WhiteLabels saveWhiteLabel(WhiteLabels whiteLabels);

    WhiteLabels updateWhiteLabel(Long id, WhiteLabels whiteLabels);

    WhiteLabels getWhiteLabelById(Long id);

    Page<WhiteLabels> getAllWhiteLabels(int page, int size);

    void deleteWhiteLabel(Long id);
}

