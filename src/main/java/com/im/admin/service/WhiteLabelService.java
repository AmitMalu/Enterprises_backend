package com.im.admin.service;

import com.im.admin.entities.WhiteLabels;

import java.util.List;

public interface WhiteLabelService {

    WhiteLabels saveWhiteLabel(WhiteLabels whiteLabel);

    WhiteLabels updateWhiteLabel(String id, WhiteLabels whiteLabel);

    List<WhiteLabels> getAllWhiteLabels();

    WhiteLabels getWhiteLabelById(String id);

    void deleteWhiteLabel(String id);
}


