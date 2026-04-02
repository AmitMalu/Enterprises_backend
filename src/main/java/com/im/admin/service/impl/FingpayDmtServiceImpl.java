package com.im.admin.service.impl;

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
import com.im.admin.service.DmtVendorService;

import java.util.List;
import java.util.Map;

public class FingpayDmtServiceImpl implements DmtVendorService {
    @Override
    public Map verifyBeneficiaryOtp(VerifyBeneOtpRequest req, VendorCredentials credentials) throws Exception {
        return Map.of();
    }

    @Override
    public Map verifyDeleteOtp(VerifyDeleteOtpRequest req, VendorCredentials credentials) throws Exception {
        return Map.of();
    }

    @Override
    public RemitterRegisterResponse verifyRemitterOtp(RemitterEkycRequest request, VendorCredentials credentials) throws Exception {
        return null;
    }

    @Override
    public RemitterQueryResponse checkRemitter( RemitterQueryRequest request, VendorCredentials credentials) throws Exception {
        return null;
    }

    @Override
    public RemitterRegisterResponse registerRemitter(RemitterRegisterRequest request, VendorCredentials credentials) throws Exception {
        return null;
    }

    @Override
    public Map doEkyc(RemitterEkycRequest request, VendorCredentials credentials) throws Exception {
        return Map.of();
    }

    @Override
    public BeneficiaryRegisterResponse registerBeneficiary(BeneficiaryRegisterRequest request, VendorCredentials credentials) throws Exception {
        return null;
    }

    @Override
    public DmtBeneficiaryDeleteResponse deleteBeneficiary(DmtBeneficiaryDeleteRequest request, VendorCredentials credentials) throws Exception {
        return null;
    }

    @Override
    public DmtBeneficiaryFetchResponse fetchBeneficiary(DmtBeneficiaryFetchRequest request, VendorCredentials credentials) throws Exception {
        return null;
    }

    @Override
    public DmtBeneficiaryFetchByBeneIdResponse fetchBeneficiaryByBeneId(DmtBeneficiaryFetchByBeneIdRequest request, VendorCredentials credentials) throws Exception {
        return null;
    }

    @Override
    public List<DmtBankList> getdmtbanklist(DmtBankListRequest request,VendorCredentials credentials) {
        return List.of();
    }

    @Override
    public String generateJwtToken() {
        return "";
    }

    @Override
    public PaysprintPennyDropResponse pennyDropVerify(PennyDropRequest request, VendorCredentials credentials, String baseUrl) {
        return null;
    }

    @Override
    public String getVendorCode() {
        return "Fingpay Dmt";
    }

    @Override
    public RemitterTransactionResponse processDmt(SendDmtRequest request, VendorCredentials credentials, String orderId, SubProducts subProducts, WalletType walletType, Double latitude, Double longitude) {
        return null;
    }

    @Override
    public PaysprintSendOtpResponse sendOtp(SendDmtRequest request, VendorCredentials credentials) {
        return null;
    }

}
