package com.im.admin.service;

import com.im.admin.entities.Partner;

import java.util.List;

public interface PartnerService {

    Partner savePartner(Partner partner);

    Partner updatePartner(String partnerId, Partner updatedPartner);

    List<Partner> getAllPartners();

    Partner getPartnerById(String partnerId);

    void deletePartner(String partnerId);
}








