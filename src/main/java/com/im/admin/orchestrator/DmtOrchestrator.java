package com.im.admin.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.DmtResponseDto;
import com.im.admin.dto.RemitterTransactionResponse;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.DmtResponseMapper;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

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

    @Value("${paysprint.partner-id}")
    private String partnerId;

    @Value("${paysprint.secret-key}")
    private String secretKey;

    @Value("${paysprint.issuer}")
    private String issuer;

    //  STEP 1: Send OTP
    public CommonResponse<PaysprintSendOtpResponse> sendDmtOtp(SendDmtRequest req) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {
            VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(routing);
            vendorId = vendor.getId();
            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

            // Fetch beneficiary details
            DmtBeneficiary beneficiary = beneficiaryRepository.findByBeneId(
                    Long.valueOf(req.getBeneId())
            );

            if (beneficiary == null) {
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

            //  Set retailer lat/long into req
            req.setLat(retailer.getLatitude());
            req.setLng(retailer.getLongitude());


            //  Generate referenceId (20 chars)
            String refId = generateReferenceId();
            req.setReferenceId(refId);

            DmtVendorService vendorService = vendorFactory.getVendor(vendor.getVendorName());
            PaysprintSendOtpResponse otpResponse = vendorService.sendOtp(req, credentials);

            otpResponse.setReferenceId(refId);


            return CommonResponse.success(otpResponse, otpResponse.getMessage());
        } catch (Exception ex) {

            log.error(
                    "[DMT SEND OTP] Exception | mobile={}, beneId={}",
                    req.getMobile(),
                    req.getBeneId(),
                    ex
            );

            return CommonResponse.error(400, "Failed to send OTP");
        }
    }

    @Transactional
    public CommonResponse<DmtResponseDto> processDmt(SendDmtRequest req) {

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
            vendorId = vendor.getId();

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


            // Fetch beneficiary details
            DmtBeneficiary beneficiary = beneficiaryRepository.findByBeneId(
                    Long.valueOf(req.getBeneId())
            );

            if (beneficiary == null) {
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

            //  Set retailer lat/long into req
            req.setLat(retailer.getLatitude());
            req.setLng(retailer.getLongitude());


            // 6. Call vendor-specific implementation (must return PayoutResponseDto)
            RemitterTransactionResponse remitterTransactionResponse = vendorService.processDmt(req, credentials, orderId, subProducts, walletType);

            // ================== VENDOR SUCCESS LOG ==================
            responseJson = objectMapper.writeValueAsString(remitterTransactionResponse);


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
                            req.getTxnType(),
                            req.getAmount().doubleValue())
                    .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + req.getAmount()));

            // Build DTO for transaction engine
            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();
            dto.setBaseTranxId(baseTxnId);
            dto.setAmount(req.getAmount().doubleValue());
            dto.setConfirmAmount(req.getAmount().doubleValue());
            dto.setOperatorName(subProducts.getSubProductTitle());
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


            log.info("[PROCESS PAYOUT ORCH] Saved pending txn: {}, vendorStatus={}", orderId, remitterTransactionResponse.getStatus());
            String vendorStatus = DmtResponseMapper.map(remitterTransactionResponse.getResponse_code());
            transactionEngineServiceImpl.processTransaction(dto, vendorStatus);

            if ("SUCCESS".equalsIgnoreCase(vendorStatus)) {

                // Fetch remitter using Optional
                Optional<DmtRemitter> optional = remitterRepository.findByMobile(req.getMobile());

                if (optional.isPresent()) {
                    DmtRemitter remitter = optional.get();

                    double usedAmount = req.getAmount().doubleValue();
                    double newLimit = remitter.getLimitAmount() - usedAmount;

                    if (newLimit < 0) {
                        newLimit = 0; // Avoid negative limit
                    }

                    remitter.setLimitAmount(newLimit);
                    remitterRepository.save(remitter);

                    log.info("[DMT LIMIT] Remitter limit updated for {} | New Limit: {}",
                            remitter.getMobile(), newLimit);
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
                    Long.valueOf(req.getBene_id())
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


//    public String generateJwtToken() {
//        long timestamp = Instant.now().getEpochSecond(); // seconds
//        String reqId = String.valueOf(Math.abs(UUID.randomUUID().hashCode()));
//
//
//        String base64Secret = TextCodec.BASE64.encode(secretKey);
//
//        //  Ensure key is at least 32 bytes (256 bits)
//        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
//
//        if (keyBytes.length < 32) {
//            keyBytes = Arrays.copyOf(keyBytes, 32); // zero-padded
//        }
//
//        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
//
//        return Jwts.builder()
//                .setIssuer(issuer)
//                .setHeaderParam("typ", "JWT")
//                .setHeaderParam("alg", "HS256")
//                .claim("timestamp", timestamp)
//                .claim("partnerId", partnerId)
//                .claim("reqid", reqId)
//                .signWith(key, SignatureAlgorithm.HS256)
//                .compact();
//    }

//    public String generateJwtToken() {
//
//        int random = getRandom();
//        long timestamp = getTimestamp();
//
//        String secretKeyBase64  =
//                "UFMwMDY0MDQ5YTk5MTYxZmI4YjgxMjYyNzBlODY2NDAxMTg3NzIxMjE3NTczMzkxMDA=";
//
//        System.out.println("JWT timestamp = " + timestamp);
//        System.out.println("JWT reqid     = " + random);
//
//        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
//
//        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
//
//        return Jwts.builder()
//                .setIssuer("PSPRINT")
//                .claim("iss", "PSPRINT")
//                .claim("timestamp", Instant.now().getEpochSecond())
//                .claim("partnerId", "PS006404")
//                .claim("product", "WALLET")
//                .claim("reqid", System.nanoTime())
//                .signWith(key, SignatureAlgorithm.HS256)
//                .compact();
//    }

    public String generateJwtToken() {

        int random = getRandom();
        long timestamp = getTimestamp();

        String jwtKey =
                "UFMwMDY0MDQ5YTk5MTYxZmI4YjgxMjYyNzBlODY2NDAxMTg3NzIxMjE3NTczMzkxMDA=";

        System.out.println("JWT timestamp = " + timestamp);
        System.out.println("JWT reqid     = " + random);

        String token = Jwts.builder()
                .setIssuer("PSPRINT")
//                .setHeaderParam("typ", "JWT")
//                .setHeaderParam("alg", "HS256")
                .claim("timestamp", timestamp)
                .claim("iat", timestamp - 720)
                .claim("partnerId", "PS006404")
                .claim("product","WALLET")
                .claim("reqid", random)
                .signWith(SignatureAlgorithm.HS256,
                        jwtKey.getBytes(StandardCharsets.UTF_8))
                .compact();

        return token;
    }

    private static int getRandom() {
        int random = (int) Math.round(Math.random() * 1000000000);
        return random;
    }

    private static long getTimestamp() {
        long timestamp = Instant.now().getEpochSecond();
        return timestamp;
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
    }



