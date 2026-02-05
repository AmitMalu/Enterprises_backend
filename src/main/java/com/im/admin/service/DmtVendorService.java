package com.im.admin.service;

//import com.im.admin.dto.DmtResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.RemitterTransactionResponse;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.request.PennyDropRequest;
import com.im.admin.request.SendDmtRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PaysprintPennyDropResponse;
import com.im.admin.response.PaysprintSendOtpResponse;

public interface DmtVendorService {

    String getVendorCode();

    RemitterTransactionResponse processDmt(SendDmtRequest request, VendorCredentials credentials, String orderId, SubProducts subProducts, WalletType walletType);

    PaysprintSendOtpResponse sendOtp(SendDmtRequest request, VendorCredentials credentials) throws JsonProcessingException;

    PaysprintPennyDropResponse pennyDropVerify(PennyDropRequest request, VendorCredentials credentials, String baseUrl);

    String generateJwtToken();
}
