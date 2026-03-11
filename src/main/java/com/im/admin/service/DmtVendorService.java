package com.im.admin.service;

//import com.im.admin.dto.DmtResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.*;
import com.im.admin.entities.DmtBankList;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.request.*;
import com.im.admin.response.BeneficiaryRegisterResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PaysprintPennyDropResponse;
import com.im.admin.response.PaysprintSendOtpResponse;

import java.util.List;
import java.util.Map;

public interface DmtVendorService {

    String getVendorCode();

    RemitterTransactionResponse processDmt(SendDmtRequest request, VendorCredentials credentials, String orderId, SubProducts subProducts, WalletType walletType);

    PaysprintSendOtpResponse sendOtp(SendDmtRequest request, VendorCredentials credentials) throws JsonProcessingException;

    PaysprintPennyDropResponse pennyDropVerify(PennyDropRequest request, VendorCredentials credentials, String baseUrl);

    String generateJwtToken();

    List<DmtBankList> getdmtbanklist();

    RemitterQueryResponse checkRemitter(
            String mobile,
            VendorCredentials credentials) throws Exception;

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
            VendorCredentials credentials) throws Exception;}
