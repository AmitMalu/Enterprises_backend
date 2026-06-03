package com.im.admin.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.DmtResponseMapper;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.*;
import com.im.admin.response.*;
import com.im.admin.service.DmtVendorService;
import com.im.admin.service.impl.PaysprintDmtServiceImpl;
import com.im.admin.service.impl.TransactionEngineServiceImpl;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.DmtVendorFactory;
import com.im.admin.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import java.time.Instant;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.crypto.dsig.Transform;

import java.nio.charset.StandardCharsets;
import java.util.Base64;


@Service
@RequiredArgsConstructor
@Slf4j
public class DmtOrchestrator {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private TokenUtil tokenUtil;

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

    @Autowired
    private RemitterRepository remitterRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private ObjectMapper objectMapper;

    RestTemplate restTemplate = new RestTemplate();

    //  STEP 1: Send OTP
    public CommonResponse<PaysprintSendOtpResponse> sendDmtOtp(SendDmtRequest req) {

        try {

            VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            List<Vendors> vendors = List.of(
                    routing.getRoutingPriority1(),
                    routing.getRoutingPriority2(),
                    routing.getRoutingPriority3()
            );

            Vendors selectedVendor = null;
            VendorCredentials credentials = null;
            DmtVendorService vendorService = null;

            // ===== VENDOR SELECTION =====
            for (Vendors v : vendors) {

                boolean allowed = checkVendorLimit(
                        req.getMobile(),
                        v.getVendorName(),
                        req.getAmount()
                );

                if (allowed) {
                    selectedVendor = v;

                    credentials = vendorCredentialsRepository
                            .findByVendorIdAndProductId(v.getId(), v.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

                    vendorService = vendorFactory.getVendor(v.getVendorName());

                    // ==========================================
                    //  IMPORTANT: IF INSTANTPAY → GET REF KEY
                    // ==========================================
                    if ("InstantPay DMT".equalsIgnoreCase(v.getVendorName())) {

                        RemitterQueryRequest remReq = new RemitterQueryRequest();
                        remReq.setMobile(req.getMobile());
                        remReq.setSubProductId(req.getSubProductId());
                        remReq.setRetailerId(req.getRetailerId());

                        CommonResponse<RemitterQueryResponse> remRes = checkRemitter(remReq);

                        if (remRes != null &&
                                remRes.getData() != null &&
                                remRes.getData().getData() != null) {

                            String referenceKey =
                                    remRes.getData().getData().getReferenceKey();

                            if (referenceKey != null) {
                                req.setReferenceKey(referenceKey);
                            }
                        }
                    }

                    break;
                }
            }

            // ===== NO VENDOR =====
            if (selectedVendor == null) {
                return CommonResponse.error(400, "All vendors monthly limit exceeded (25K)");
            }

            DmtRemitter dmtRemitter = remitterRepository
                    .findByMobile(req.getMobile())
                    .orElseThrow(() ->
                            new RuntimeException("Remitter not found")
                    );

            // ===== FETCH BENEFICIARY =====
            DmtBeneficiary beneficiary = beneficiaryRepository
                    .findByRemitterAndBeneId(dmtRemitter, req.getBeneId())
                    .orElseThrow(() ->
                            new RuntimeException("Beneficiary not found")
                    );

            if (beneficiary == null) {
                return CommonResponse.error(400, "Beneficiary not found");
            }

            // ===== FETCH RETAILER =====
            Retailer retailer = retailerRepository.findByRetailerId(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            // ===== SET DATA =====
            req.setDob(beneficiary.getDob());
            req.setAddress(beneficiary.getAddress());
            req.setPincode(beneficiary.getPincode());
            req.setGstState(beneficiary.getGst_state());

            req.setLat(retailer.getLatitude());
            req.setLng(retailer.getLongitude());

            // ===== GENERATE REF ID =====
            String refId = generateReferenceId();
            req.setReferenceId(refId);

            // ================================
            // FIRST ATTEMPT
            // ================================
            PaysprintSendOtpResponse otpResponse =
                    vendorService.sendOtp(req, credentials);

            // ================================
            // HANDLE INVALID REF KEY
            // ================================
            if (otpResponse != null &&
                    otpResponse.getMessage() != null &&
                    otpResponse.getMessage().equalsIgnoreCase("Invalid or expired referenceKey")) {

                RemitterQueryRequest remReq = new RemitterQueryRequest();
                remReq.setMobile(req.getMobile());
                remReq.setSubProductId(req.getSubProductId());

                CommonResponse<RemitterQueryResponse> remRes = checkRemitter(remReq);

                String newReferenceKey = null;

                if (remRes != null &&
                        remRes.getData() != null &&
                        remRes.getData().getData() != null) {

                    newReferenceKey = remRes.getData().getData().getReferenceKey();
                }

                if (newReferenceKey != null) {
                    req.setReferenceKey(newReferenceKey);

                    //  RETRY
                    otpResponse = vendorService.sendOtp(req, credentials);
                }
            }

            // ===== SAVE SELECTED VENDOR =====
            beneficiary.setVendor(selectedVendor);
            beneficiary.setUpdatedAt(LocalDateTime.now());
            beneficiaryRepository.save(beneficiary);

            otpResponse.setReferenceId(refId);

            return CommonResponse.success(otpResponse, otpResponse.getMessage());

        } catch (Exception ex) {

            log.error("[DMT SEND OTP] Exception | mobile={}, beneId={}",
                    req.getMobile(),
                    req.getBeneId(),
                    ex
            );

            return CommonResponse.error(400, ex.getMessage());
        }
    }

    private boolean checkVendorLimit(String mobile, String vendorId, BigDecimal amount) {

        double MAX_LIMIT = 25000.0;

        DmtRemitter remitter = remitterRepository.findByMobile(mobile)
                .orElseThrow(() -> new RuntimeException("Remitter not found"));

        //  Monthly reset logic
        LocalDateTime lastUpdated = remitter.getUpdatedAt();

        if (lastUpdated == null || lastUpdated.getMonthValue() != LocalDateTime.now().getMonthValue()) {
            // reset all vendor limits if new month
            remitter.resetMonthlyLimits();   // you create this method
            remitterRepository.save(remitter);
        }

        double used = remitter.getUsedLimitForVendor(vendorId);

        return (used + amount.doubleValue()) <= MAX_LIMIT;
    }

    @Transactional
    public CommonResponse<DmtResponseDto> processDmt(SendDmtRequest req, Double latitude, Double longitude) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

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

            // Fetch matching SchemeRate
            log.info("[2] Searching SchemeRate for schemeCode={}, operatorName={}, amount={}",
                    req.getSchemeCode(), req.getTxnType(), req.getAmount());

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            req.getSchemeCode(),
                            req.getTxnType(),
                            req.getAmount().doubleValue())
                    .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + req.getAmount()));

            log.info("[2.1] Found SchemeRate: ID={}, RateType={}, SlabType={}, Gross={}, RetailerRate={}",
                    selectedScheme.getId(), selectedScheme.getRateType(), selectedScheme.getSlabType(),
                    selectedScheme.getGross(), selectedScheme.getRetailer());

            BigDecimal finalDeductAmt = req.getAmount().add(BigDecimal.valueOf(selectedScheme.getGross()));

            if (availableBalance.compareTo(finalDeductAmt) < 0) {
                log.warn("[PROCESS DMT] Insufficient balance for retailer {} (required={}, available={})",
                        req.getRetailerId(), req.getAmount(), availableBalance);
                return new CommonResponse<>(400, "Insufficient Balance", null);
            }

            //** imp
            DmtRemitter dmtRemitter = remitterRepository
                    .findByMobile(req.getMobile())
                    .orElseThrow(() ->
                            new RuntimeException("Remitter not found")
                    );

            // ===== FETCH BENEFICIARY =====
            DmtBeneficiary beneficiary = beneficiaryRepository
                    .findByRemitterAndBeneId(dmtRemitter, req.getBeneId())
                    .orElseThrow(() ->
                            new RuntimeException("Beneficiary not found")
                    );

            Vendors vendor = beneficiary.getVendor(); //  MAIN FIX

            vendorId = vendor.getId();
            req.setVendorName(vendor.getVendorName());  // used becoz to addusedlimit down on Succcess

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
            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

            String schemeCode = selectedScheme.getSchemeCode();

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
                    "Fund Transferred #" + req.getAccountNumber(),
                    schemeCode,
                    latitude,
                    longitude
            );

            log.debug("[PROCESS DMT] Wallet debited (PENDING) for retailer={}, amount={}",
                    req.getRetailerId(), req.getAmount());

            // 6️ CALL Vendor API
            log.info("[PROCESS DMT] Calling vendor API for orderId={}, vendor={}",
                    orderId, vendor.getVendorName());

            // ===== FETCH BENEFICIARY =====
            DmtBeneficiary afterBeneficiary = beneficiaryRepository
                    .findByRemitterAndBeneId(dmtRemitter, req.getBeneId())
                    .orElseThrow(() ->
                            new RuntimeException("Beneficiary not found")
                    );

            if (afterBeneficiary == null) {
                return CommonResponse.error(400, "Beneficiary not found");
            }

            //  Fetch retailer details
            Retailer retailer = retailerRepository.findByRetailerId(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            //  Set beneficiary data into req
            req.setDob(beneficiary.getDob());
            req.setAddress(beneficiary.getAddress());
            req.setPincode(beneficiary.getPincode());
            req.setGstState(beneficiary.getGst_state());
            req.setAccountNumber(beneficiary.getAccountNo());
            req.setIfscCode(afterBeneficiary.getIfscCode());

            //  Set retailer lat/long into req
            req.setLat(retailer.getLatitude());
            req.setLng(retailer.getLongitude());

            // 6. Call vendor-specific implementation (must return PayoutResponseDto)
            RemitterTransactionResponse remitterTransactionResponse = vendorService.processDmt(req, credentials, orderId, subProducts, walletType, latitude, longitude);

            // ================== VENDOR SUCCESS LOG ==================
            responseJson = objectMapper.writeValueAsString(remitterTransactionResponse);

            // Save vendor transaction details locally
            DmtTransaction dmtTxn = new DmtTransaction();
            dmtTxn.setTransactionType(req.getTxnType());
            dmtTxn.setOrderId(orderId);
            dmtTxn.setAckno(remitterTransactionResponse.getAckno());
            dmtTxn.setUtr(remitterTransactionResponse.getUtr());
            dmtTxn.setResponseCode(remitterTransactionResponse.getResponse_code());
            dmtTxn.setTxnStatus(remitterTransactionResponse.getTxn_status());
            dmtTxn.setVendorStatus(DmtResponseMapper.map(remitterTransactionResponse.getResponse_code(), remitterTransactionResponse.getMessage()));
            dmtTxn.setBeneName(remitterTransactionResponse.getBenename());
            dmtTxn.setRemarks(remitterTransactionResponse.getRemarks());
            dmtTxn.setMessage(remitterTransactionResponse.getMessage());
            dmtTxn.setRemitter(remitterTransactionResponse.getRemitter());
            dmtTxn.setAccountNumber(remitterTransactionResponse.getAccount_number());
            dmtTxn.setTxnAmount(remitterTransactionResponse.getTxn_amount());
            dmtTxn.setBalance(remitterTransactionResponse.getBalance());
            dmtTxn.setReferenceId(
                    remitterTransactionResponse.getReferenceid() != null
                            ? remitterTransactionResponse.getReferenceid()
                            : null
            );

            // save to DB
            dmtTransactionRepository.save(dmtTxn);

            log.info("[PROCESS DMT] Transaction saved locally | orderId={} | status={}",
                    orderId, dmtTxn.getVendorStatus());

            // Build DTO for transaction engine
            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();
            dto.setBaseTranxId(baseTxnId);
            dto.setAmount(req.getAmount().doubleValue());
            dto.setConfirmAmount(req.getAmount().doubleValue());
            dto.setOperatorName(req.getTxnType());
            dto.setOperatorCode(selectedScheme.getOperatorCode());
            dto.setSchemeRate(selectedScheme);

            Products product = subProducts.getProduct();
            //dto.setTransactionType(product.getTransactionType());
            dto.setTransactionType("Debit");
            dto.setWalletType(subProducts.getWalletType());
            dto.setGstValue(String.valueOf(product.getSgst()));
            dto.setTdsRate(String.valueOf(product.getTds()));
            dto.setProductCode(product.getProductCode());
            dto.setSubProductCode(subProducts.getSubProductCode());
            dto.setIsCommission(product.getIsCommissions());
            dto.setHasCharge(product.getHasCharges());
            dto.setHierarchyId(req.getRetailerId());
            dto.setLatitude(Double.valueOf(latitude));
            dto.setLongitude(Double.valueOf(longitude));


            log.info("[PROCESS PAYOUT ORCH] Saved pending txn: {}, vendorStatus={}", orderId, remitterTransactionResponse.getStatus());
            String vendorStatus = DmtResponseMapper.map(remitterTransactionResponse.getResponse_code(), remitterTransactionResponse.getMessage());
            transactionEngineServiceImpl.processTransaction(dto, vendorStatus);

            if ("SUCCESS".equalsIgnoreCase(vendorStatus)) {

                Optional<DmtRemitter> optional = remitterRepository.findByMobile(req.getMobile());

                if (optional.isPresent()) {
                    DmtRemitter remitter = optional.get();

                    // USE SAME VENDOR FROM OTP
                    String vendorName = req.getVendorName();

                    remitter.addUsedLimit(vendorName, req.getAmount().doubleValue());

                    remitter.setUpdatedAt(LocalDateTime.now());

                    remitterRepository.save(remitter);

                    log.info("[DMT LIMIT] Updated | mobile={} | vendor={} | amount={}",
                            remitter.getMobile(), vendorName, req.getAmount());
                }
            }

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
                    .ipay_uuid(remitterTransactionResponse.getIpay_uuid())
                    .referenceId(remitterTransactionResponse.getReferenceid() != null
                            ? String.valueOf(remitterTransactionResponse.getReferenceid())
                            : null)
                    .balance(remitterTransactionResponse.getBalance() != null
                            ? BigDecimal.valueOf(remitterTransactionResponse.getBalance())
                            : null)
                    .build();

            //  Return wrapped success response
            if ("SUCCESS".equalsIgnoreCase(vendorStatus)) {
                return CommonResponse.success(responseDto, "Transaction Successful");
            } else if ("PENDING".equalsIgnoreCase(vendorStatus)) {
                return new CommonResponse<>(202, "Transaction Pending", responseDto);
            } else {
                return new CommonResponse<>(400, "Transaction Failed", responseDto);
            }


        } catch (Exception e) {

            log.error("[PROCESS DMT] Exception occurred | orderId={}", orderId, e);
            return new CommonResponse<>(500, "Error while processing DMT", null);
        }
    }

    public CommonResponse<PaysprintPennyDropResponse> pennyDropVerify(PennyDropRequest req) {
        try {

            VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            if (routing == null) {
                return CommonResponse.error(400, "Vendor routing not found");
            }

            Vendors vendor = CommonMethods.getVendors(routing);
            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

            // ---------------------------
            // Fetch Beneficiary
            // ---------------------------
            DmtBeneficiary beneficiary = beneficiaryRepository.findByBeneId(
                    (req.getBene_id())
            );

            if (beneficiary == null) {
                return CommonResponse.error(400, "Beneficiary not found");
            }

            // ---------------------------
            // Fetch Retailer
            // ---------------------------
            Retailer retailer = retailerRepository.findByRetailerId(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            // ---------------------------
            // Merge DB Values Into req
            // ---------------------------
            req.setDob(beneficiary.getDob());
            req.setAddress(beneficiary.getAddress());
            req.setPincode(beneficiary.getPincode());
            req.setGst_state(beneficiary.getGst_state());

            req.setLat(retailer.getLatitude());
            req.setLng(retailer.getLongitude());

            // Generate 20-char reference ID
            String refId = generateReferenceId();
            req.setReferenceid(refId);

            // Vendor service
            DmtVendorService vendorService = vendorFactory.getVendor(vendor.getVendorName());

            PaysprintPennyDropResponse resp =
                    vendorService.pennyDropVerify(req, credentials, credentials.getBaseUrlProd());


            return CommonResponse.success(resp, resp.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return CommonResponse.error(400, "Exception: " + e.getMessage());
        }
    }


    public String generateReferenceId() {
        String digits = String.valueOf(System.currentTimeMillis()).substring(3);
        String rand = String.format("%06d", new Random().nextInt(999999));
        return "REF" + digits + rand;  // 20 characters
    }

    public String encrypt(String json, String aesKey, String aesIv) {

        try {
            String plainText = json;
            String key = aesKey;
            String iv = aesIv;

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            SecretKeySpec secretKey =
                    new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

            IvParameterSpec ivSpec =
                    new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted =
                    cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            throw new RuntimeException("AES encryption error", e);
        }
    }

    public CommonResponse<RemitterQueryResponse> checkRemitter(
            RemitterQueryRequest request
    ) throws Exception {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {

            String hierarchyId = tokenUtil.extractHierarchyId();
            request.setRetailerId(hierarchyId);

            // ===== STEP 1 Vendor Routing =====
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(request.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            vendorId = vendor.getId();

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() ->
                                    new RuntimeException("No credentials found for selected vendor"));

            // ====== Check Vendor Type ======
            if ("AES".equalsIgnoreCase(credentials.getTokenType())) {

                // AES Flow (Admin Panel)
                String token = commonMethods.generateTokenForAES(credentials);

                // Build JSON request for AES admin panel
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("client_id", credentials.getClientId());
                headers.set("token", token);
                headers.add("Authorization", "Bearer " + token); // <--- ADD THIS

                ApiPanelRemitterRequest jsonBody =
                        new ApiPanelRemitterRequest();
                jsonBody.setMobileNo(request.getMobile());

                jsonBody.setProductId(
                        vendor.getProduct().getId().toString());
                jsonBody.setApiPartnerId(credentials.getUsername());

                HttpEntity<ApiPanelRemitterRequest> requestEntity = new HttpEntity<>(jsonBody, headers);

                String url = credentials.getBaseUrlUat() + "/remitter/check";
                log.info("AES Admin panel order URL: {}", url);

                ResponseEntity<CommonResponse<RemitterQueryResponse>> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                requestEntity,
                                new ParameterizedTypeReference<CommonResponse<RemitterQueryResponse>>() {}
                        );

                responseJson = String.valueOf(response.getBody());

                commonFunctions.saveVendorLogs(
                        LocalDateTime.now(),
                        "AES order request",
                        vendor.getId(),
                        LocalDateTime.now(),
                        responseJson,
                        null
                );

                return response.getBody();
            }

            // ===== STEP 2 LOCAL DB CHECK =====
            Optional<DmtRemitter> exist =
                    remitterRepository.findByMobileAndVendor(request.getMobile(), vendor);

            //  ONLY for PAYSPRINT → use DB shortcut
            if (!"InstantPay DMT".equalsIgnoreCase(vendor.getVendorName())) {

                if (exist.isPresent() &&
                        "REGISTERED".equals(exist.get().getStatus())) {

                    DmtRemitter remitter = exist.get();

                    RemitterQueryResponse res = new RemitterQueryResponse();
                    res.setStatus(true);
                    res.setResponse_code(1);
                    res.setMessage("Remitter account details fetched.!");

                    RemitterQueryResponse.DataObj data =
                            new RemitterQueryResponse.DataObj();

                    data.setMobile(remitter.getMobile());

                    double totalLimit = remitter.getLimitAmount() != null
                            ? remitter.getLimitAmount()
                            : 25000.0;

                    double usedLimit = remitter.getUsedLimitForVendor(vendor.getVendorName());

                    double availableLimit = totalLimit - usedLimit;

                    data.setLimit(availableLimit);

                    res.setData(data);

                    return CommonResponse.success(
                            res,
                            "Remitter already registered and active"
                    );
                }
            }

            // ===== STEP 3 CALL VENDOR SERVICE =====

            Map<String, Object> reqBody = Map.of("mobile", request.getMobile());
            requestJson = objectMapper.writeValueAsString(reqBody);

            DmtVendorService vendorService =
                    vendorFactory.getVendor(vendor.getVendorName());

            RemitterQueryResponse res =
                    vendorService.checkRemitter(request, credentials);

            res.setVendor_flow(vendor.getVendorName());

            responseJson = objectMapper.writeValueAsString(res);

            // ===== SUCCESS VENDOR LOG =====
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            int code = res.getResponse_code();

            // ===== STEP 4 SAME SWITCH LOGIC =====

            switch (code) {

                case 1:
                    saveRemitter(res, vendor);
                    return CommonResponse.success(
                            res,
                            "Remitter exists. You can proceed"  //0
                    );

                case 2:
                    return CommonResponse.success(
                            res,
                            "EKYC_REQUIRED"
                    );

                case 3:

                    // SAVE RNF (IMPORTANT)
                    saveRemitterForRNF(request.getMobile(), res, vendor);

                    // RESPONSE BASED ON VENDOR
                    if ("InstantPay DMT".equalsIgnoreCase(vendor.getVendorName())) {

                        return CommonResponse.success(
                                res,
                                "Remitter not found. Please proceed with registration"
                        );

                    } else {

                        return CommonResponse.success(
                                res,
                                "OTP sent. Please register remitter"
                        );
                    }

                default:
                    return CommonResponse.handleErrors(
                            res.getMessage()
                    );
            }

        } catch (Exception ex) {

            // ===== FAILURE VENDOR LOG =====
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    null,
                    ex.getMessage()
            );

            log.error("[CHECK REMITTER] Exception | mobile={}", request.getMobile(), ex);
            throw ex;
        }
    }


    private void saveRemitter(RemitterQueryResponse res, Vendors vendor) {

        String mobile = res.getData().getMobile();

        Optional<DmtRemitter> existing =
                remitterRepository.findByMobile(mobile);

        if (existing.isPresent()) {
            // Already exists → just update status
            DmtRemitter rem = existing.get();
            rem.setLimitAmount(res.getData().getLimit());
            rem.setStatus("REGISTERED");

            if (res.getData().getReferenceKey() != null) {
                rem.setReferenceKey(res.getData().getReferenceKey());
            }

            remitterRepository.save(rem);
            return;
        }

        // First time insert
        DmtRemitter en = new DmtRemitter();
        en.setMobile(mobile);
        en.setStatus("REGISTERED");
        en.setCreatedAt(LocalDateTime.now());

        // keep global limit if needed (optional)
        en.setLimitAmount(res.getData().getLimit());

        if (res.getData().getReferenceKey() != null) {
            en.setReferenceKey(res.getData().getReferenceKey());
        }

        // initialize vendor limits (optional safety)
        en.setVendor1UsedLimit(0.0);
        en.setVendor2UsedLimit(0.0);
        en.setVendor3UsedLimit(0.0);

        remitterRepository.save(en);
    }

    // new
    private void saveRemitterForRNF(String mobile,
                                    RemitterQueryResponse res,
                                    Vendors vendor) {

        Optional<DmtRemitter> existing =
                remitterRepository.findByMobile(mobile);

        String referenceKey = null;

        if (res.getData() != null) {
            referenceKey = res.getData().getReferenceKey();
        }

        if (existing.isPresent()) {

            DmtRemitter rem = existing.get();

            //  DO NOT override REGISTERED
            if ("REGISTERED".equalsIgnoreCase(rem.getStatus())) {
                return;
            }

            // Update referenceKey
            if (referenceKey != null) {
                rem.setReferenceKey(referenceKey);
            }

            rem.setStatus("NOT_REGISTERED");
            rem.setUpdatedAt(LocalDateTime.now());

            remitterRepository.save(rem);
            return;
        }

        //  NEW ENTRY
        DmtRemitter en = new DmtRemitter();

        en.setMobile(mobile);
        en.setStatus("NOT_REGISTERED");
        en.setCreatedAt(LocalDateTime.now());

        if (referenceKey != null) {
            en.setReferenceKey(referenceKey);
        }

        // default values
        en.setLimitAmount(0.0);
        en.setVendor1UsedLimit(0.0);
        en.setVendor2UsedLimit(0.0);
        en.setVendor3UsedLimit(0.0);

        remitterRepository.save(en);
    }

    public CommonResponse<RemitterRegisterResponse> registerRemitter(
            RemitterRegisterRequest req) {

        String hierarchyId = tokenUtil.extractHierarchyId();
        req.setRetailerId(hierarchyId);

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {

            // STEP 1 Vendor Routing
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);
            vendorId = vendor.getId();

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("No credentials found"));

            requestJson = objectMapper.writeValueAsString(req);

            // ====== Check Vendor Type ======
            if ("AES".equalsIgnoreCase(credentials.getTokenType())) {

                // AES Flow (Admin Panel)
                String token = commonMethods.generateTokenForAES(credentials);

                // Build JSON request for AES admin panel
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("client_id", credentials.getClientId());
                headers.set("token", token);
                headers.add("Authorization", "Bearer " + token); // <--- ADD THIS

                ApiPanelRemitterRegisterRequest jsonBody = new ApiPanelRemitterRegisterRequest();

                jsonBody.setMobile(req.getMobile());
                jsonBody.setOtp(req.getOtp());
                jsonBody.setStateresp(req.getStateresp());
                jsonBody.setEkyc_id(req.getEkyc_id());
                jsonBody.setReferenceKey(req.getReferenceKey());
                jsonBody.setAadhaarNumber(req.getAadhaarNumber());
                jsonBody.setProductId(String.valueOf(credentials.getVendor().getProduct().getId()));
                jsonBody.setApiPartnerId(credentials.getUsername());

                HttpEntity<ApiPanelRemitterRegisterRequest> requestEntity = new HttpEntity<>(jsonBody, headers);

                String url = credentials.getBaseUrlUat() + "/remitter/register";
                log.info("AES Admin panel remitter register URL: {}", url);

                ResponseEntity<CommonResponse<RemitterRegisterResponse>> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                requestEntity,
                                new ParameterizedTypeReference<CommonResponse<RemitterRegisterResponse>>() {}
                        );

                responseJson = String.valueOf(response.getBody());

                commonFunctions.saveVendorLogs(
                        LocalDateTime.now(),
                        "AES order request",
                        vendor.getId(),
                        LocalDateTime.now(),
                        responseJson,
                        null
                );

                return response.getBody();
            }

            // STEP 2 Vendor Call
            DmtVendorService vendorService =
                    vendorFactory.getVendor(vendor.getVendorName());

            RemitterRegisterResponse resp =
                    vendorService.registerRemitter(req, credentials);

            responseJson = objectMapper.writeValueAsString(resp);

            // SUCCESS LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            // STEP 3: DB UPDATE (VENDOR BASED)

            if (resp != null && (resp.getResponse_code() == 1 || resp.getResponse_code() == 2)) {

                DmtRemitter rem = remitterRepository
                        .findByMobile(req.getMobile())
                        .orElse(new DmtRemitter());

                rem.setMobile(req.getMobile());
                rem.setVendor(vendor);

                //  DIFFERENT LOGIC
                if ("InstantPay DMT".equalsIgnoreCase(vendor.getVendorName())) {

                    rem.setStatus("PENDING");   // OTP sent only

                    if (resp.getData() != null) {
                        rem.setOtpReference(resp.getData().getOtpReference());
                    }

                } else if ("Paysprint DMT".equalsIgnoreCase(vendor.getVendorName())) {

                    rem.setStatus("REGISTERED"); // direct registration
                }

                // COMMON FIELDS
                rem.setEkycId(req.getEkyc_id());
                rem.setStateResp(req.getStateresp());

                if (resp.getData() != null) {
                    rem.setLimitAmount(resp.getData().getLimit());
                }

                remitterRepository.save(rem);
            }

            return CommonResponse.success(resp, resp.getMessage());

        } catch (Exception ex) {

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    ex.getMessage(),
                    null

            );

            log.error("[REGISTER REMITTER] Exception | mobile={}",
                    req.getMobile(), ex);

            throw new RuntimeException(ex);
        }
    }

    //paysprint
    public CommonResponse<Object> validateOtp(RemitterEkycRequest request) {

        try {

            String hierarchyId = tokenUtil.extractHierarchyId();
            request.setRetailerId(hierarchyId);

            VendorRouting routing =
                    vendorRoutingRepository.findBySubProductId(request.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(routing);

            if (!"InstantPay DMT".equalsIgnoreCase(vendor.getVendorName())) {
                return CommonResponse.error(400, "OTP validation only for InstantPay");
            }

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("No credentials found"));

            DmtVendorService vendorService =
                    vendorFactory.getVendor(vendor.getVendorName());

            // CALL VERIFY OTP ONLY
            RemitterRegisterResponse otpRes =
                    vendorService.verifyRemitterOtp(request, credentials);

            if (otpRes == null || otpRes.getResponse_code() != 1) {
                return CommonResponse.error(
                        400,
                        "OTP Verification Failed: " +
                                (otpRes != null ? otpRes.getMessage() : "No response")
                );
            }

            // UPDATE DB STATUS → OTP VERIFIED (optional step)
            Optional<DmtRemitter> optional =
                    remitterRepository.findByMobile(request.getMobile());

            if (optional.isPresent()) {
                DmtRemitter rem = optional.get();

                rem.setStatus("OTP_VERIFIED"); // optional intermediate state
                rem.setUpdatedAt(LocalDateTime.now());
                rem.setVendor(vendor);

                remitterRepository.save(rem);
            }

            return CommonResponse.success(null, "OTP Verified Successfully");

        } catch (Exception ex) {
            return CommonResponse.error(500, ex.getMessage());
        }
    }

    public CommonResponse<Object> doEkyc(RemitterEkycRequest request) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {

            String hierarchyId = tokenUtil.extractHierarchyId();
            request.setRetailerId(hierarchyId);

            // STEP 1: Vendor Routing
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(request.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);
            vendorId = vendor.getId();

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("No credentials found"));

            requestJson = objectMapper.writeValueAsString(request);

            DmtVendorService vendorService =
                    vendorFactory.getVendor(vendor.getVendorName());

            Map responseBody;

            // ==========================================
            // STEP 3: CALL EKYC (COMMON)
            // ==========================================
            responseBody = vendorService.doEkyc(request, credentials);

            responseJson = objectMapper.writeValueAsString(responseBody);

            // LOG SUCCESS
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            if ("Paysprint DMT".equalsIgnoreCase(vendor.getVendorName())) {

                int responseCode = (int) responseBody.get("response_code");

                if (responseCode == 1) {

                    Map data = (Map) responseBody.get("data");

                    String mobile = (String) data.get("mobile");

                    Optional<DmtRemitter> existing =
                            remitterRepository.findByMobile(mobile);

                    if (existing.isPresent()) {
                        return CommonResponse.success(
                                responseBody,
                                "E-KYC already done for this mobile"
                        );
                    }

                    DmtRemitter rem = new DmtRemitter();

                    rem.setMobile(mobile);
                    rem.setEkycId((String) data.get("ekyc_id"));
                    rem.setStateResp((String) data.get("stateresp"));
                    rem.setStatus("PENDING");   // Paysprint logic
                    rem.setCreatedAt(LocalDateTime.now());
                    rem.setName(request.getName());
                    rem.setVendor(vendor);
                    rem.setAadhaar(request.getAadhaarNumber());

                    remitterRepository.save(rem);
                }

            } else if ("InstnatPay DMT".equalsIgnoreCase(vendor.getVendorName())) {

                String statusCode = (String) responseBody.get("statuscode");

                if ("TXN".equalsIgnoreCase(statusCode)) {

                    Optional<DmtRemitter> existing =
                            remitterRepository.findByMobile(request.getMobile());

                    if (existing.isPresent()) {

                        DmtRemitter rem = existing.get();

                        rem.setStatus("REGISTERED");   //FINAL SUCCESS
                        rem.setUpdatedAt(LocalDateTime.now());
                        rem.setVendor(vendor);

                        remitterRepository.save(rem);
                    }
                }
            }

            return CommonResponse.success(responseBody, "E-KYC Success");

        } catch (Exception ex) {

            // LOG FAILURE
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    null,
                    ex.getMessage()
            );

            log.error("[DO EKYC] Exception | mobile={}", request.getMobile(), ex);

            throw new RuntimeException(ex);
        }
    }

    public CommonResponse<BeneficiaryRegisterResponse> registerBeneficiary(
            BeneficiaryRegisterRequest req) throws Exception {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {

            String hierarchyId = tokenUtil.extractHierarchyId();
            req.setRetailerId(hierarchyId);


            // STEP 1 Vendor Routing
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            vendorId = vendor.getId();

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("No credentials found"));

            requestJson = objectMapper.writeValueAsString(req);

            // STEP 2 Vendor Call
            DmtVendorService vendorService =
                    vendorFactory.getVendor(vendor.getVendorName());


            // STEP 0: GET REMITTER
            DmtRemitter remitter = remitterRepository
                    .findByMobile(req.getMobile())
                    .orElseThrow(() -> new RuntimeException("Remitter not found"));

// STEP 0.1: CHECK EXISTING BENEFICIARY BEFORE VENDOR CALL
            Optional<DmtBeneficiary> existingBefore =
                    beneficiaryRepository.findByRemitterAndAccountNo(
                            remitter,
                            req.getAccno()
                    );


            if (existingBefore.isPresent()) {
                DmtBeneficiary existingBene = existingBefore.get();

                // BLOCK IF ALREADY REGISTERED
                if ("REGISTERED".equalsIgnoreCase(existingBene.getStatus())) {
                    return CommonResponse.success(null, "Beneficiary already exists");
                }
            }


            BeneficiaryRegisterResponse resp =
                    vendorService.registerBeneficiary(req, credentials);

            responseJson = objectMapper.writeValueAsString(resp);

            // SUCCESS VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            // ===============================
            // SAVE LOGIC (CLEAN & CORRECT)
            // ===============================
            if (resp != null) {

                // ==========================================
                // CASE 1: SUCCESS (Paysprint OR InstantPay TXN)
                // ==========================================
                if (resp.getResponse_code() == 1) {

                    String beneId = resp.getData() != null
                            ? resp.getData().getBene_id()
                            : null;

                    Optional<DmtBeneficiary> existing =
                            beneficiaryRepository.findByRemitterAndBeneId(remitter, beneId);

                    DmtBeneficiary bene;

                    if (existing.isPresent()) {

                        bene = existing.get();

                        // BLOCK if already registered
                        if ("REGISTERED".equalsIgnoreCase(bene.getStatus())) {
                            return CommonResponse.success(resp, "Beneficiary already exists");
                        }

                    } else {

                        bene = new DmtBeneficiary();
                        bene.setRemitter(remitter);
                        bene.setVendor(vendor);
                        bene.setBeneId(beneId);
                    }

                    // COMMON FIELDS
                    bene.setName(req.getBenename());
                    bene.setBankId(req.getBankid());
                    bene.setBankName(req.getBankName());
                    bene.setAccountNo(req.getAccno());
                    bene.setIfscCode(req.getIfsccode());

                    // FINAL STATUS
                    bene.setStatus("REGISTERED");

                    beneficiaryRepository.save(bene);
                }

                // ==========================================
                // CASE 2: INSTANTPAY OTP (DO NOTHING)
                // ==========================================
                else if (resp.getResponse_code() == 2) {

                    //  DO NOT INSERT
                    // Only return response
                    return CommonResponse.success(resp, resp.getMessage());
                }
            }

            return CommonResponse.success(resp, resp.getMessage());

        } catch (Exception ex) {

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    null,
                    ex.getMessage()
            );

            throw ex;
        }
    }

    public CommonResponse<DmtBeneficiaryDeleteResponse> deleteBeneficiary(
            DmtBeneficiaryDeleteRequest req) throws Exception {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {

            String hierarchyId = tokenUtil.extractHierarchyId();
            req.setRetailerId(hierarchyId);

            // STEP 1 Vendor Routing
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            vendorId = vendor.getId();

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("No credentials found"));

            requestJson = objectMapper.writeValueAsString(req);

            // STEP 2 Vendor Call
            DmtVendorService vendorService =
                    vendorFactory.getVendor(vendor.getVendorName());

            DmtBeneficiaryDeleteResponse vendorResponse =
                    vendorService.deleteBeneficiary(req, credentials);

            responseJson = objectMapper.writeValueAsString(vendorResponse);

            // SUCCESS VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            // STEP 3 Update Local DB
            if (vendorResponse != null) {

                // SUCCESS → delete from DB
                if (vendorResponse.getResponse_code() == 1) {

                    Optional<DmtBeneficiary> optionalBene =
                            beneficiaryRepository.findByBeneIdAndRemitter_Mobile(
                                    req.getBene_id(),
                                    req.getMobile()
                            );

                    if (optionalBene.isPresent()) {
                        beneficiaryRepository.delete(optionalBene.get());
                    }
                }

                // OTP CASE → store referenceKey
                else if (vendorResponse.getResponse_code() == 2) {

                    Optional<DmtBeneficiary> optionalBene =
                            beneficiaryRepository.findByBeneIdAndRemitter_Mobile(
                                    req.getBene_id(),
                                    req.getMobile()
                            );

                    if (optionalBene.isPresent()) {

                        DmtBeneficiary bene = optionalBene.get();

                        bene.setReferenceKey(vendorResponse.getReferenceKey());
                        bene.setStatus("DELETE_PENDING");
                        bene.setUpdatedAt(LocalDateTime.now());

                        beneficiaryRepository.save(bene);
                    }
                }
            }

            return CommonResponse.success(
                    vendorResponse,
                    vendorResponse != null
                            ? vendorResponse.getMessage()
                            : "No response from vendor"
            );

        } catch (Exception ex) {

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    null,
                    ex.getMessage()
            );

            log.error("[DELETE BENEFICIARY] Exception | beneId={}",
                    req.getBene_id(), ex);

            throw ex;
        }
    }

    public CommonResponse<Page<DmtBeneficiaryFetchResponse.BeneficiaryData>> fetchBeneficiary(
            DmtBeneficiaryFetchRequest req, int page, int size) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {

            Optional<DmtRemitter> optionalRemitter =
                    remitterRepository.findByMobile(req.getMobile());

            if (optionalRemitter.isEmpty()) {
                return CommonResponse.handleErrors("Remitter not found");
            }

            DmtRemitter remitter = optionalRemitter.get();

            // =============================
            // STEP 1 → Fetch Local DB
            // =============================
            List<DmtBeneficiary> localList =
                    beneficiaryRepository.findByRemitterOrderByIdDesc(remitter);

            if (!localList.isEmpty()) {

                List<DmtBeneficiaryFetchResponse.BeneficiaryData> dtoList =
                        localList.stream().map(this::convertToDto).toList();

                List<DmtBeneficiaryFetchResponse.BeneficiaryData> filteredList =
                        applyFilters(dtoList, req);

                int start = (int) PageRequest.of(page, size).getOffset();
                int end = Math.min(start + size, filteredList.size());

                List<DmtBeneficiaryFetchResponse.BeneficiaryData> pagedData =
                        filteredList.subList(start, end);

                Page<DmtBeneficiaryFetchResponse.BeneficiaryData> pageResponse =
                        new PageImpl<>(pagedData, PageRequest.of(page, size), filteredList.size());

                return CommonResponse.success(pageResponse, "Beneficiaries fetched");
            }

            // =============================
            // STEP 2 → Vendor Routing
            // =============================
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            vendorId = vendor.getId();

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("No credentials found"));

            requestJson = objectMapper.writeValueAsString(req);

            // =============================
            // STEP 3 → Vendor API Call
            // =============================
            DmtVendorService vendorService =
                    vendorFactory.getVendor(vendor.getVendorName());

            DmtBeneficiaryFetchResponse resp =
                    vendorService.fetchBeneficiary(req, credentials);

            responseJson = objectMapper.writeValueAsString(resp);

            // SUCCESS LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            if (resp == null) {
                return CommonResponse.handleErrors("No response from vendor");
            }

            // =============================
            // STEP 4 → Pagination
            // =============================
            if (resp.getResponse_code() == 1 && resp.getData() != null) {

                List<DmtBeneficiaryFetchResponse.BeneficiaryData> beneficiaries =
                        resp.getData();

                List<DmtBeneficiaryFetchResponse.BeneficiaryData> filteredList =
                        applyFilters(beneficiaries, req);

                int start = (int) PageRequest.of(page, size).getOffset();
                int end = Math.min(start + size, filteredList.size());

                List<DmtBeneficiaryFetchResponse.BeneficiaryData> pagedList =
                        filteredList.subList(start, end);

                Page<DmtBeneficiaryFetchResponse.BeneficiaryData> pagedResponse =
                        new PageImpl<>(pagedList, PageRequest.of(page, size), filteredList.size());

                return CommonResponse.success(
                        pagedResponse,
                        "Beneficiaries fetched successfully"
                );

            } else {
                return CommonResponse.handleErrors(resp.getMessage());
            }

        } catch (Exception ex) {

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    null,
                    ex.getMessage()
            );

            log.error("[FETCH BENEFICIARY] Exception | mobile={}",
                    req.getMobile(), ex);

            return CommonResponse.handleErrors(
                    "Error fetching beneficiary list: " + ex.getMessage()
            );
        }
    }

    private DmtBeneficiaryFetchResponse.BeneficiaryData convertToDto(DmtBeneficiary b) {

        DmtBeneficiaryFetchResponse.BeneficiaryData dto =
                new DmtBeneficiaryFetchResponse.BeneficiaryData();

        dto.setBene_id(String.valueOf(b.getBeneId()));
        dto.setBankid(b.getBankId());
        dto.setBankname(b.getBankName());
        dto.setName(b.getName());
        dto.setAccno(b.getAccountNo());
        dto.setIfsc(b.getIfscCode());
        dto.setVerified(b.getVerified());
        dto.setBanktype(null);
        dto.setPaytm(false);

        return dto;
    }

    private List<DmtBeneficiaryFetchResponse.BeneficiaryData> applyFilters(
            List<DmtBeneficiaryFetchResponse.BeneficiaryData> list,
            DmtBeneficiaryFetchRequest req) {

        return list.stream()
                .filter(b -> req.getBeneId() == null
                        || b.getBene_id().equals(req.getBeneId()))

                .filter(b -> req.getBankid() == null
                        || b.getBankid().equals(req.getBankid()))

                .filter(b -> req.getBankname() == null
                        || b.getBankname().toLowerCase()
                        .contains(req.getBankname().toLowerCase()))

                .filter(b -> req.getName() == null
                        || b.getName().toLowerCase()
                        .contains(req.getName().toLowerCase()))

                .filter(b -> req.getAccno() == null
                        || b.getAccno().equals(req.getAccno()))

                .filter(b -> req.getIfsc() == null
                        || b.getIfsc().equalsIgnoreCase(req.getIfsc()))

                .filter(b -> req.getVerified() == null
                        || b.getVerified().equals(req.getVerified()))

                .filter(b -> req.getPaytm() == null
                        || b.isPaytm() == req.getPaytm())

                .toList();
    }


    public CommonResponse<DmtBeneficiaryFetchByBeneIdResponse> fetchBeneficiaryByBeneId(
            DmtBeneficiaryFetchByBeneIdRequest req) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {

            // STEP 1 Vendor Routing
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            vendorId = vendor.getId();

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("No credentials found"));

            requestJson = objectMapper.writeValueAsString(req);

            // STEP 2 Vendor Call
            DmtVendorService vendorService =
                    vendorFactory.getVendor(vendor.getVendorName());

            DmtBeneficiaryFetchByBeneIdResponse resp =
                    vendorService.fetchBeneficiaryByBeneId(req, credentials);

            responseJson = objectMapper.writeValueAsString(resp);

            // SUCCESS LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            if (resp == null) {
                return CommonResponse.handleErrors("No response from vendor");
            }

            if (resp.getResponse_code() == 1) {
                return CommonResponse.success(resp, "Beneficiary fetched successfully");
            } else {
                return CommonResponse.handleErrors(resp.getMessage());
            }

        } catch (Exception ex) {

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    null,
                    ex.getMessage()
            );

            log.error(
                    "[FETCH BENEFICIARY BY BENE ID] Exception | mobile={}, beneId={}",
                    req.getMobile(),
                    req.getBeneid(),
                    ex
            );

            return CommonResponse.handleErrors(
                    "Error fetching beneficiary details: " + ex.getMessage()
            );
        }
    }

    public CommonResponse<Object> verifyBeneficiaryOtp(VerifyBeneOtpRequest req) throws Exception {
        try {

            String hierarchyId = tokenUtil.extractHierarchyId();
            req.setRetailerId(hierarchyId);

            // ===== GET VENDOR =====
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("No credentials"));

            DmtVendorService vendorService =
                    vendorFactory.getVendor(vendor.getVendorName());

            // ===== CALL VENDOR =====
            Map resp = vendorService.verifyBeneficiaryOtp(req, credentials);

            String statusCode = (String) resp.get("statuscode");

            // ===== SUCCESS =====
            if ("TXN".equalsIgnoreCase(statusCode)) {

                // ===== FETCH REMITTER =====
                DmtRemitter remitter = remitterRepository
                        .findByMobile(req.getRemitterMobile())
                        .orElseThrow(() -> new RuntimeException("Remitter not found"));

                // ===== CHECK EXISTING =====
                Optional<DmtBeneficiary> existing =
                        beneficiaryRepository.findByRemitterAndBeneId(remitter, req.getBeneId());

                DmtBeneficiary bene;

                if (existing.isPresent()) {
                    bene = existing.get();
                } else {
                    // INSERT ONLY AFTER OTP SUCCESS
                    bene = new DmtBeneficiary();
                    bene.setRemitter(remitter);
                    bene.setVendor(vendor);
                    bene.setBeneId(req.getBeneId());
                    bene.setCreatedAt(LocalDateTime.now()); // optional
                }

                // ===== SET ALL FIELDS (IMPORTANT) =====
                bene.setName(req.getBenename());
                bene.setBankId(req.getBankid());
                bene.setBankName(req.getBankName());
                bene.setAccountNo(req.getAccno());
                bene.setIfscCode(req.getIfsccode());

                bene.setVerified(req.getVerified());
                bene.setDob(req.getDob());
                bene.setAddress(req.getAddress());
                bene.setPincode(req.getPincode());
                bene.setGst_state(req.getGst_state());
                bene.setReferenceKey(req.getReferenceKey());


                // ===== FINAL STATUS =====
                bene.setStatus("REGISTERED");
                bene.setUpdatedAt(LocalDateTime.now());

                beneficiaryRepository.save(bene);

                return CommonResponse.success(resp, "Beneficiary Registered Successfully");
            }

            // ===== FAILURE =====
            return CommonResponse.error(
                    400,
                    (String) resp.get("status")
            );

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public CommonResponse<Object> verifyDeleteOtp(VerifyDeleteOtpRequest req) throws Exception {

        String hierarchyId = tokenUtil.extractHierarchyId();
        req.setRetailerId(hierarchyId);

        // FETCH BENEFICIARY (SAFE WAY)
        Optional<DmtBeneficiary> optionalBene =
                beneficiaryRepository.findByBeneIdAndRemitter_Mobile(
                        req.getBene_id(),
                        req.getMobile()
                );

        if (optionalBene.isEmpty()) {
            throw new RuntimeException("Beneficiary not found");
        }

        DmtBeneficiary bene = optionalBene.get();

        // GET VENDOR
        Vendors vendor = bene.getVendor();

        VendorCredentials credentials =
                vendorCredentialsRepository
                        .findByVendorIdAndProductId(
                                vendor.getId(),
                                vendor.getProduct().getId())
                        .orElseThrow(() -> new RuntimeException("No credentials found"));

        DmtVendorService vendorService =
                vendorFactory.getVendor(vendor.getVendorName());

        // CALL VENDOR
        Map resp = vendorService.verifyDeleteOtp(req, credentials);

        if (resp == null) {
            throw new RuntimeException("Empty response from vendor");
        }

        String statusCode = (String) resp.get("statuscode");

        // ===============================
        //  SUCCESS → DELETE FROM DB
        // ===============================
        if ("TXN".equalsIgnoreCase(statusCode)) {

            beneficiaryRepository.delete(bene);

            return CommonResponse.success(resp, "Beneficiary Deleted Successfully");
        }

        // ===============================
        // FAILURE
        // ===============================
        return CommonResponse.error(
                400,
                resp.get("status") != null
                        ? resp.get("status").toString()
                        : "Delete OTP verification failed"
        );
    }

    public CommonResponse<List<DmtBankList>> getBankList(DmtBankListRequest request) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {

            String hierarchyId = tokenUtil.extractHierarchyId();
            request.setRetailerId(hierarchyId);

            // ===== STEP 1: Vendor Routing =====
            VendorRouting routing =
                    vendorRoutingRepository.findBySubProductId(request.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(routing);
            vendorId = vendor.getId();

            // ===== STEP 2: Get Credentials =====
            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() ->
                                    new RuntimeException("No credentials found"));

            // ===== STEP 3: Call Vendor =====
            DmtVendorService vendorService =
                    vendorFactory.getVendor(vendor.getVendorName());

            List<DmtBankList> banks =
                    vendorService.getdmtbanklist(request, credentials);

            responseJson = objectMapper.writeValueAsString(banks);

            // ===== LOG SUCCESS =====
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            return CommonResponse.success(
                    banks,
                    "Bank list fetched successfully"
            );

        } catch (Exception ex) {

            // ===== LOG FAILURE =====
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    ex.getMessage()
            );

            throw new RuntimeException("Bank list fetch failed", ex);
        }
    }
}