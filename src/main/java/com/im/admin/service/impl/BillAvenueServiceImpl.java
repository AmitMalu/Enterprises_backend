package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.BbpsTranactionRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.BillFetchRequest;
import com.im.admin.request.BillPaymentRequest;
import com.im.admin.request.BillerRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BillAvenueService;
import com.im.admin.service.DmtVendorService;
import com.im.admin.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
public class BillAvenueServiceImpl implements BillAvenueService {

    @Autowired
    private BillPayConfigRepository billPayConfigRepository;


    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;


    @Autowired
    private BBPSRelatedTransactions bbpsRelatedTransactions;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private DmtVendorFactory vendorFactory;

    @Autowired
    private SubProductsRepository subProductsRepository;


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


    private static final String ACCESS_CODE = "AVUU30TF96SP77ZJJH";
    private static final String INSTITUTE_ID = "NF07";
    private static final String WORKING_KEY = "063F0699C1D46414EA1E92D0E6BDCF99";

    private static final String API_URL =
            "https://stgapi.billavenue.com/billpay/extMdmCntrl/mdmRequestNew/json";

    @Override
    public Object fetchBillerInfo(BillerRequest request) throws Exception {

        String requestedBillerId = request.getBillerId().get(0); // first biller id

        // ----------------------------
        // 1. Check in DB before calling vendor API
        // ----------------------------
        Optional<BillPayConfig> existing =
                billPayConfigRepository.findByProviderId(requestedBillerId);

        if (existing.isPresent()) {

            // Return saved raw_config JSON (convert to JsonNode)
            ObjectMapper mapper = new ObjectMapper();
            JsonNode savedJson = mapper.readTree(existing.get().getRawConfig());

            return CommonResponse.success(savedJson, "Biller info fetched");
        }


        // ----------------------------
        // 2. NOT FOUND → Hit vendor API
        // ----------------------------

        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(request.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);

        ObjectMapper mapper = new ObjectMapper();

        // Step 1: Convert request → JSON string
        String plainJson = mapper.writeValueAsString(request);

        // Step 2: Encrypt
        String encryptedHex = BillAvenueCryptoUtil.encrypt(plainJson, WORKING_KEY);

        // Step 3: requestId
        String requestId = RequestIdUtil.generate();

        // Step 4: Build API URL
        String finalUrl = API_URL
                + "?accessCode=" + ACCESS_CODE
                + "&requestId=" + requestId
                + "&ver=1.0"
                + "&instituteId=" + INSTITUTE_ID;

        // Step 5: Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        HttpEntity<String> entity = new HttpEntity<>(encryptedHex, headers);

        // Step 6: Call API
        RestTemplate restTemplate = new RestTemplate();
        String encryptedResponse = restTemplate.postForObject(finalUrl, entity, String.class);

        // Step 7: Decrypt
        String decrypt = BillAvenueCryptoUtil.decrypt(encryptedResponse, WORKING_KEY);

        // Step 8: Convert decrypted text → JSON
        JsonNode jsonNode = mapper.readTree(decrypt);


        // ----------------------------
        // 3. Save into DB
        // ----------------------------

        JsonNode billerArray = jsonNode.path("biller");

        if (billerArray.isArray() && billerArray.size() > 0) {

            JsonNode biller = billerArray.get(0);

            BillPayConfig cfg = new BillPayConfig();

            cfg.setVendor(vendor.getVendorName());
            cfg.setServiceName(biller.path("billerCategory").asText());
            cfg.setProviderId(biller.path("billerId").asText());
            cfg.setProviderName(biller.path("billerName").asText());
            cfg.setRawConfig(biller.toString());

            billPayConfigRepository.save(cfg);
        }

        return CommonResponse.success(jsonNode, "Biller info fetched from Vendor API");
    }


    @Override
    public Object fetchBill(BillFetchRequest request) throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        // Step 1: Convert request to JSON string
        String plainJson = mapper.writeValueAsString(request);

        // Step 2: Encrypt JSON
        String encryptedRequest = BillAvenueCryptoUtil.encrypt(plainJson, WORKING_KEY);

        // Step 3: Generate requestId
        String requestId = RequestIdUtil.generate();

        // Step 4: Prepare final URL (encRequest included as query param)
        String finalUrl =
                "https://stgapi.billavenue.com/billpay/extBillCntrl/billFetchRequest/j" +
                        "son"
                        + "?accessCode=" + ACCESS_CODE
                        + "&requestId=" + requestId
                        + "&ver=1.0"
                        + "&instituteId=" + INSTITUTE_ID
                        + "&encRequest=" + encryptedRequest;   // 👈 URL PARAM

        // Step 5: Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        //  Empty POST body
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        RestTemplate restTemplate = new RestTemplate();

        // Step 6: POST API call (EMPTY BODY)
        String encryptedResponse = restTemplate.postForObject(
                finalUrl,
                entity,
                String.class
        );

        // Step 7: Decrypt response
        String decrypted = BillAvenueCryptoUtil.decrypt(encryptedResponse, WORKING_KEY);

        // Step 8: Convert decrypted JSON to object
        return mapper.readTree(decrypted);
    }


    @Override
    public Object doBillPayment(BillPaymentRequest request) throws Exception {

        String orderId = "TXN" + commonMethods.generateBaseTransactionId();
        log.info("[PROCESS BILL PAYMENT] Starting transaction | orderId={}", orderId);

        try {
            //  Validate SubProduct
            SubProducts subProducts = subProductsRepository.findById(request.getSubProductId())
                    .orElseThrow(() ->
                            new RuntimeException("SubProduct not found for ID: " + request.getSubProductId()));

            WalletType walletType = WalletType.valueOf(subProducts.getWalletType());
            log.debug("[PROCESS BILL PAYMENT] Resolved walletType={}", walletType);

            // 2 Check Retailer Balance
            BigDecimal availableBalance =
                    commonMethods.getCurrentBalance(request.getRetailerId(), walletType.name());

            String baseTransactionId = commonMethods.generateBaseTransactionId();
            log.info("Generated baseTransactionId: {}", baseTransactionId);

            BigDecimal amount = request.getAmountInfo().getAmount();

            if (availableBalance.compareTo(amount) < 0) {
                log.warn("[PROCESS BILL PAYMENT] Insufficient balance | retailerId={}, required={}, available={}",
                        request.getRetailerId(), amount, availableBalance);
                return new CommonResponse<>(400, "Insufficient Balance", null);
            }

            //  Vendor Routing
            VendorRouting routing =
                    vendorRoutingRepository.findBySubProductId(request.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(routing);
            log.info("Vendor selected: {}", vendor.getVendorName());

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            log.debug("Vendor credentials retrieved for vendorId={}", vendor.getId());

            String retailerId = request.getRetailerId();
//            Double amount = request.getAmountInfo().getAmount().doubleValue();

            // 4 Wallet Object (PENDING debit)
            AbstractWalletAmountTable walletAmountTable =
                    commonMethods.getWalletByType(walletType.name(), request.getRetailerId());

            String baseTxnId = orderId.startsWith("TXN")
                    ? orderId.substring(3)
                    : orderId;

            Products product = subProducts.getProduct();
            String gstValue = String.valueOf(product.getSgst());
            String tdsValue = String.valueOf(product.getTds());


            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            request.getSchemeCode(),
                            request.getTxnType(),
                            request.getAmountInfo().getAmount().doubleValue())
                    .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + request.getAmountInfo().getAmount()));

            BbpsTranactionRequestDto bbpsTranactionRequestDto = new BbpsTranactionRequestDto(selectedScheme, amount.doubleValue(), walletType.name(), retailerId, gstValue, tdsValue, "PENDING", baseTransactionId, selectedScheme.getOperatorName(), subProducts.getSubProductTitle());
            commonMethods.updateWallet(walletAmountTable, retailerId, walletType, request.getAmountInfo().getAmount(), "Debit", baseTransactionId, "PENDING", "DEBIT", subProducts.getSubProductTitle(), "Tx Amount for Bill Avenue Billpay For "  +"#" ,"Bill Avenue  Billpay Transaction from " + walletType.name());
            bbpsRelatedTransactions.processCharges(bbpsTranactionRequestDto, walletAmountTable);

            // 5️ Convert request → JSON
            ObjectMapper mapper = new ObjectMapper();
            String plainJson = mapper.writeValueAsString(request);

            // 6⃣ Encrypt JSON
            String encryptedRequest =
                    BillAvenueCryptoUtil.encrypt(plainJson, WORKING_KEY);

            // 7⃣ Generate requestId
            String requestId = RequestIdUtil.generate();

            // 8️ Build BILL PAYMENT URL
            String finalUrl =
                    "https://stgapi.billavenue.com/billpay/extBillPayCntrl/billPayRequest/json"
                            + "?accessCode=" + ACCESS_CODE
                            + "&requestId=" + requestId
                            + "&ver=1.0"
                            + "&instituteId=" + INSTITUTE_ID
                            + "&encRequest=" + encryptedRequest;

            // 9 Headers (EMPTY BODY POST)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>("", headers);

            RestTemplate restTemplate = new RestTemplate();

            // Call Bill Payment API
            String encryptedResponse =
                    restTemplate.postForObject(finalUrl, entity, String.class);

            // 1 Decrypt response
            String decrypted =
                    BillAvenueCryptoUtil.decrypt(encryptedResponse, WORKING_KEY);

            //  Return JSON
            return mapper.readTree(decrypted);

        } catch (Exception ex) {
            log.error("[PROCESS BILL PAYMENT] Error occurred | orderId={}", orderId, ex);
            throw ex;
        }
    }

}


