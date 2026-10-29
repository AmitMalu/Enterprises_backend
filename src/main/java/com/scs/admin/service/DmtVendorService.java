package com.scs.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scs.admin.dto.*;
import com.scs.admin.entities.DmtBankList;
import com.scs.admin.entities.SubProducts;
import com.scs.admin.entities.VendorCredentials;
import com.scs.admin.entities.enums.WalletType;
import com.scs.admin.request.*;
import com.scs.admin.response.BeneficiaryRegisterResponse;
import com.scs.admin.response.PaysprintPennyDropResponse;
import com.scs.admin.response.PaysprintSendOtpResponse;

import java.util.List;
import java.util.Map;

public interface DmtVendorService {

    String getVendorCode();

    RemitterTransactionResponse processDmt(SendDmtRequest request, VendorCredentials credentials, String orderId, SubProducts subProducts, WalletType walletType, Double latitude, Double longitude);

    PaysprintSendOtpResponse sendOtp(SendDmtRequest request, VendorCredentials credentials) throws JsonProcessingException;

    PaysprintPennyDropResponse pennyDropVerify(PennyDropRequest request, VendorCredentials credentials, String baseUrl);

    String generateJwtToken();

    List<DmtBankList> getdmtbanklist(DmtBankListRequest request, VendorCredentials credentials);

    RemitterQueryResponse checkRemitter(
            RemitterQueryRequest request,
            VendorCredentials credentials
    ) throws Exception;

    RemitterRegisterResponse registerRemitter(
            RemitterRegisterRequest request,
            VendorCredentials credentials) throws Exception;

    Map doEkyc(
            RemitterEkycRequest request,
            VendorCredentials credentials) throws Exception;

    BeneficiaryRegisterResponse registerBeneficiary(
            BeneficiaryRegisterRequest request,
            VendorCredentials credentials) throws Exception;


    DmtBeneficiaryDeleteResponse deleteBeneficiary(
            DmtBeneficiaryDeleteRequest request,
            VendorCredentials credentials) throws Exception;

    DmtBeneficiaryFetchResponse fetchBeneficiary(
            DmtBeneficiaryFetchRequest request,
            VendorCredentials credentials) throws Exception;

    DmtBeneficiaryFetchByBeneIdResponse fetchBeneficiaryByBeneId(
            DmtBeneficiaryFetchByBeneIdRequest request,
            VendorCredentials credentials) throws Exception;

    RemitterRegisterResponse verifyRemitterOtp(RemitterEkycRequest request, VendorCredentials credentials) throws Exception;

    Map verifyBeneficiaryOtp(VerifyBeneOtpRequest req, VendorCredentials credentials) throws Exception;

    Map verifyDeleteOtp(VerifyDeleteOtpRequest req, VendorCredentials credentials) throws Exception;
}
