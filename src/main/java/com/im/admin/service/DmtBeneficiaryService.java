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
import org.springframework.data.domain.Page;

public interface DmtBeneficiaryService {
    CommonResponse<BeneficiaryRegisterResponse> registerBeneficiary(BeneficiaryRegisterRequest request);
    CommonResponse<DmtBeneficiaryDeleteResponse> deleteBeneficiary(DmtBeneficiaryDeleteRequest request);

    CommonResponse<Page<DmtBeneficiaryFetchResponse.BeneficiaryData>> fetchBeneficiary(DmtBeneficiaryFetchRequest req, int page, int size);



    CommonResponse<DmtBeneficiaryFetchByBeneIdResponse> fetchBeneficiaryByBeneId(DmtBeneficiaryFetchByBeneIdRequest req);
}
