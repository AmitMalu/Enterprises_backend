package com.scs.admin.service;



import com.scs.admin.dto.AutoChargeMasterRequestDTO;
import com.scs.admin.entities.ChargeMaster;

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

