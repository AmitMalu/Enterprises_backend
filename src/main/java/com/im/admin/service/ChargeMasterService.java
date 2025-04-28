package com.im.admin.service;



import com.im.admin.dto.AutoChargeMasterRequestDTO;
import com.im.admin.entities.ChargeMaster;

import java.util.List;

public interface ChargeMasterService {

    ChargeMaster createChargeMaster(ChargeMaster chargeMaster);

    ChargeMaster updateChargeMaster(Long id, ChargeMaster chargeMaster);

    void deleteChargeMaster(Long id);

    ChargeMaster getChargeMaster(Long id);

    List<ChargeMaster> getAllChargeMasters();

    double calculateTotalAmount(Double amount, Double gst);

    String applyAutoCharge(AutoChargeMasterRequestDTO request);
}

