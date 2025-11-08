package com.im.admin.service;

import com.im.admin.dto.DmtBeneficiaryDeleteResponse;
import com.im.admin.dto.DmtBeneficiaryFetchByBeneIdResponse;
import com.im.admin.dto.DmtBeneficiaryFetchResponse;
import com.im.admin.request.BeneficiaryRegisterRequest;
import com.im.admin.request.DmtBeneficiaryDeleteRequest;
import com.im.admin.request.DmtBeneficiaryFetchByBeneIdRequest;
import com.im.admin.request.DmtBeneficiaryFetchRequest;
import com.im.admin.response.BeneficiaryRegisterResponse;
import com.im.admin.response.CommonResponse;

public interface DmtBeneficiaryService {
    CommonResponse<BeneficiaryRegisterResponse> registerBeneficiary(BeneficiaryRegisterRequest request);
    CommonResponse<DmtBeneficiaryDeleteResponse> deleteBeneficiary(DmtBeneficiaryDeleteRequest request);

    CommonResponse<DmtBeneficiaryFetchResponse> fetchBeneficiary(DmtBeneficiaryFetchRequest req);

    CommonResponse<DmtBeneficiaryFetchByBeneIdResponse> fetchBeneficiaryByBeneId(DmtBeneficiaryFetchByBeneIdRequest req);
}
