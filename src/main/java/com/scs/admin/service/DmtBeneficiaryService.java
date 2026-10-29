package com.scs.admin.service;

import com.scs.admin.dto.DmtBeneficiaryDeleteResponse;
import com.scs.admin.dto.DmtBeneficiaryFetchByBeneIdResponse;
import com.scs.admin.dto.DmtBeneficiaryFetchResponse;
import com.scs.admin.entities.DmtBeneficiary;
import com.scs.admin.request.*;
import com.scs.admin.response.BeneficiaryRegisterResponse;
import com.scs.admin.response.CommonResponse;
import org.springframework.data.domain.Page;

public interface DmtBeneficiaryService {
    CommonResponse<BeneficiaryRegisterResponse> registerBeneficiary(BeneficiaryRegisterRequest request)throws Exception;

    CommonResponse<DmtBeneficiaryDeleteResponse> deleteBeneficiary(DmtBeneficiaryDeleteRequest request)throws Exception;

    CommonResponse<Page<DmtBeneficiaryFetchResponse.BeneficiaryData>> fetchBeneficiary(DmtBeneficiaryFetchRequest req, int page, int size);



    CommonResponse<DmtBeneficiaryFetchByBeneIdResponse> fetchBeneficiaryByBeneId(DmtBeneficiaryFetchByBeneIdRequest req);

    CommonResponse<DmtBeneficiaryResponseDto> updateBeneficiary(BeneficiaryUpdateRequest request);
}
