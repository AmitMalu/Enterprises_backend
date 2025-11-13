package com.im.admin.orchestrator;

import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.DmtResponseDto;
import com.im.admin.dto.RemitterTransactionResponse;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.DmtTransactionProcessingDto;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.PennyDropRequest;
import com.im.admin.request.SendDmtRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PaysprintPennyDropResponse;
import com.im.admin.response.PaysprintSendOtpResponse;
import com.im.admin.service.DmtVendorService;
import com.im.admin.service.impl.PaysprintDmtServiceImpl;
import com.im.admin.service.impl.TransactionEngineServiceImpl;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.DmtVendorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DmtOrchestrator {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private DmtVendorFactory vendorFactory;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private DmtTransactionRepository dmtTransactionRepository;


    //  STEP 1: Send OTP
    public CommonResponse<PaysprintSendOtpResponse> sendDmtOtp(SendDmtRequest req) {
        try {
            VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(routing);
            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

            DmtVendorService vendorService = vendorFactory.getVendor(vendor.getVendorName());
            PaysprintSendOtpResponse otpResponse = vendorService.sendOtp(req, credentials);

            return CommonResponse.success(otpResponse, otpResponse.getMessage());
        } catch (Exception e) {
            log.error("[DMT ORCH] Error in sendDmtOtp: {}", e.getMessage(), e);
            return CommonResponse.error(400,"Failed to send OTP");
        }
    }



    public CommonResponse<DmtResponseDto> processDmt(SendDmtRequest req) {
        String orderId = "TXN" + commonMethods.generateBaseTransactionId();
        log.info("[PROCESS DMT] Starting transaction | orderId={}", orderId);

        try {
            //Validate SubProduct
            SubProducts subProducts = subProductsRepository.findById(req.getSubProductId())
                    .orElseThrow(() -> new RuntimeException("SubProduct not found for ID: " + req.getSubProductId()));

            WalletType walletType = WalletType.valueOf(subProducts.getWalletType());
            log.debug("[PROCESS DMT] Resolved walletType={}", walletType);

            // Check Retailer Balance
            BigDecimal availableBalance = commonMethods.getCurrentBalance(req.getRetailerId(), walletType.name());
            if (availableBalance.compareTo(req.getAmount()) < 0) {
                log.warn("[PROCESS DMT] Insufficient balance for retailer {} (required={}, available={})",
                        req.getRetailerId(), req.getAmount(), availableBalance);
                return new CommonResponse<>(400, "Insufficient Balance", null);
            }

            // 3⃣ Resolve Vendor Routing & Credentials
            VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            if (routing == null) {
                log.error("[PROCESS DMT] Vendor routing not found for subProductId={}", req.getSubProductId());
                return new CommonResponse<>(404, "Vendor routing not found", null);
            }

            Vendors vendor = CommonMethods.getVendors(routing);
            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

            // 4️ Get Vendor Service Bean (Paysprint )
            DmtVendorService vendorService = vendorFactory.getVendor(vendor.getVendorName());
            if (vendorService == null) {
                log.error("[PROCESS DMT] No vendor implementation found for vendorName={}", vendor.getVendorName());
                return new CommonResponse<>(500, "Vendor implementation not found", null);
            }

            // 5️ Wallet Debit (PENDING)
            AbstractWalletAmountTable walletAmountTable =
                    commonMethods.getWalletByType(walletType.name(), req.getRetailerId());
//            String baseTxnId = orderId.substring(3);
            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

            commonMethods.updateWallet(
                    walletAmountTable,
                    req.getRetailerId(),
                    walletType,
                    req.getAmount(),
                    "Debit",
                    baseTxnId,
                    "PENDING",
                    "DEBIT",
                    subProducts.getSubProductTitle(),
                    req.getBeneficiaryName() + " #" + req.getAccountNumber(),
                    "Fund Transferred #" + req.getAccountNumber()
            );

            log.debug("[PROCESS DMT] Wallet debited (PENDING) for retailer={}, amount={}",
                    req.getRetailerId(), req.getAmount());

            // 6️ CALL Vendor API
            log.info("[PROCESS DMT] Calling vendor API for orderId={}, vendor={}",
                    orderId, vendor.getVendorName());

            // 6. Call vendor-specific implementation (must return PayoutResponseDto)
            RemitterTransactionResponse remitterTransactionResponse = vendorService.processDmt(req, credentials, orderId, subProducts, walletType);
//            if (vendorResp == null) {
//                // defensive fallback
//                vendorResp = DmtResponseDto.builder()
//                        .orderId(orderId)
//                        .vendorCode(vendor.getVendorCode())
//                        .status("FAILED")
//                        .message("Empty response from vendor")
//                        .amount(req.getAmount())
//                        .build();
//            }

            // Save vendor transaction details locally
            DmtTransaction dmtTxn = new DmtTransaction();
            dmtTxn.setOrderId(orderId);
            dmtTxn.setAckno(remitterTransactionResponse.getAckno());
            dmtTxn.setUtr(remitterTransactionResponse.getUtr());
            dmtTxn.setResponseCode(remitterTransactionResponse.getResponse_code());
            dmtTxn.setTxnStatus(remitterTransactionResponse.getTxn_status());
            dmtTxn.setVendorStatus(DmtResponseMapper.map(remitterTransactionResponse.getResponse_code()));
            dmtTxn.setBeneName(remitterTransactionResponse.getBenename());
            dmtTxn.setRemarks(remitterTransactionResponse.getRemarks());
            dmtTxn.setMessage(remitterTransactionResponse.getMessage());
            dmtTxn.setRemitter(remitterTransactionResponse.getRemitter());
            dmtTxn.setAccountNumber(remitterTransactionResponse.getAccount_number());
            dmtTxn.setTxnAmount(remitterTransactionResponse.getTxn_amount());
            dmtTxn.setBalance(remitterTransactionResponse.getBalance());
            dmtTxn.setReferenceId(
                    remitterTransactionResponse.getReferenceid() != null
                            ? String.valueOf(remitterTransactionResponse.getReferenceid())
                            : null
            );

               // save to DB
            dmtTransactionRepository.save(dmtTxn);

            log.info("[PROCESS DMT] Transaction saved locally | orderId={} | status={}",
                    orderId, dmtTxn.getVendorStatus());





            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            req.getSchemeCode(),
                            req.getTransactionType(),
                            req.getAmount().doubleValue() )
                    .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + req.getAmount()));

           // Build DTO for transaction engine
//            DmtTransactionProcessingDto dto = new DmtTransactionProcessingDto();
            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();
            dto.setBaseTranxId(baseTxnId);
            dto.setAmount(req.getAmount().doubleValue());
            dto.setConfirmAmount(req.getAmount().doubleValue());
            dto.setOperatorName(subProducts.getSubProductTitle());
            dto.setOperatorCode(selectedScheme.getOperatorCode());
            dto.setSchemeRate(selectedScheme);

            Products product = subProducts.getProduct();
            dto.setTransactionType(product.getTransactionType());
            dto.setWalletType(subProducts.getWalletType());
            dto.setGstValue(String.valueOf(product.getSgst()));
            dto.setTdsRate(String.valueOf(product.getTds()));
            dto.setProductCode(product.getProductCode());
            dto.setSubProductCode(subProducts.getSubProductCode());
            dto.setIsCommission(product.getIsCommissions());
            dto.setHasCharge(product.getHasCharges());
            dto.setHierarchyId(req.getRetailerId());


            log.info("[PROCESS PAYOUT ORCH] Saved pending txn: {}, vendorStatus={}", orderId, remitterTransactionResponse.getStatus());
            String vendorStatus = DmtResponseMapper.map(remitterTransactionResponse.getResponse_code());
            transactionEngineServiceImpl.processTransaction(dto, vendorStatus);

          //  Build response DTO
            DmtResponseDto responseDto = DmtResponseDto.builder()
                    .orderId(orderId)
                    .vendorTxnId(remitterTransactionResponse.getAckno())
                    .amount(req.getAmount())
                    .status(vendorStatus)
                    .message(remitterTransactionResponse.getMessage())
                    .beneficiaryName(remitterTransactionResponse.getBenename())
                    .remarks(remitterTransactionResponse.getRemarks())
                    .utr(remitterTransactionResponse.getUtr())
                    .referenceId(remitterTransactionResponse.getReferenceid() != null
                            ? String.valueOf(remitterTransactionResponse.getReferenceid())
                            : null)
                    .balance(remitterTransactionResponse.getBalance() != null
                            ? BigDecimal.valueOf(remitterTransactionResponse.getBalance())
                            : null)
                    .build();

            //  Return wrapped success response
            return CommonResponse.success(responseDto, "DMT transaction processed successfully");


        } catch (Exception e) {
            log.error("[PROCESS DMT] Exception occurred: {}", e.getMessage(), e);
            return new CommonResponse<>(500, "Error while processing DMT", null);
        }
    }

    public CommonResponse<PaysprintPennyDropResponse> pennyDropVerify(PennyDropRequest req) {
        try {
            VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            if (routing == null) {
                return CommonResponse.error(400,"Vendor routing not found");
            }

            Vendors vendor = CommonMethods.getVendors(routing);
            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

            DmtVendorService vendorService = vendorFactory.getVendor(vendor.getVendorName());

            if (vendorService instanceof PaysprintDmtServiceImpl) {
                PaysprintPennyDropResponse resp =  vendorService
                        .pennyDropVerify(req, credentials, credentials.getBaseUrlUat());
                return CommonResponse.success(resp, resp.getMessage());
            } else {
                return CommonResponse.error(400,"Invalid vendor for penny drop");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return CommonResponse.error(400,"Exception: " + e.getMessage());
        }
    }


    public enum DmtResponseMapper {
        SUCCESS(Set.of(1)),
        PENDING(Set.of(2, 3, 5, 6)),
        FAILED(Set.of(4,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25));

        private final Set<Integer> codes;
        DmtResponseMapper(Set<Integer> codes) {
            this.codes = codes;
        }

        public static String map(int code) {
            for (DmtResponseMapper type : values()) {
                if (type.codes.contains(code)) {
                    return type.name();
                }
            }
            return "PENDING"; // default
        }
    }

}
