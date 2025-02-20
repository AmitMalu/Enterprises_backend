package com.im.admin.service;

import com.im.admin.entities.Partner;

import java.util.List;

public interface PartnerService {

    Partner savePartner(Partner partner);

    Partner updatePartner(Long id, Partner partner);

    List<Partner> getAllPartners();

    Partner getPartnerById(Long id);

    void deletePartner(Long id);
}







