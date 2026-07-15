package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.mapper.StatusMapper;
import com.im.admin.paymentgateway.PGCreateOrderCommand;
import com.im.admin.repositories.*;
import com.im.admin.request.CashfreeCreateOrderRequest;
import com.im.admin.request.IMCallbackRequest;
import com.im.admin.request.ImPgCreateOrderRequest;
import com.im.admin.request.NixaSoftCallbackRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgStatusResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import com.im.admin.service.ImPGService;
import com.im.admin.util.CommonMethods;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@Slf4j
public class ImPGServiceImpl implements ImPGService {


    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeAllocationRepository schemeAllocationRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionServiceImpl;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private PgTransactionMappingRepository pgTransactionMappingRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private StatusMapper statusMapper;

    RestTemplate restTemplate = new RestTemplate();


    @Override
    public PgUnifiedOrderResponse createOrder(ImPgCreateOrderRequest req, RequestHeaderMetaDto requestHeaderMetaDtoMeta) {


        log.info("Starting Cashfree Payment Link | subProductId={}, amount={}",
                req.getSubProductId(), req.getAmount());
        String imResponseBody = null;

        try {

            // ===== 1. Get Vendor Credentials =====
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("No credentials found"));

            SubProducts subProduct = subProductsRepository.findById(req.getSubProductId())
                    .orElseThrow(() -> new RuntimeException("SubProduct not found"));

            // ===== 2. Generate Link ID =====
            String linkId = "ORD" + commonMethods.generateBaseTransactionId();

            if("AES".equals(credentials.getTokenType())) {

                String token = commonMethods.generateTokenForAES(credentials);

                // Build JSON request for AES admin panel
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("client_id", credentials.getClientId());
                headers.set("token", token);
                headers.add("Authorization", "Bearer " + token); // <--- ADD THIS
                headers.add("Latitude", String.valueOf(requestHeaderMetaDtoMeta.getLatitude()));
                headers.add("Longitude", String.valueOf(requestHeaderMetaDtoMeta.getLongitude()));

                PGCreateOrderCommand command = new PGCreateOrderCommand();

                command.setAmount(Double.valueOf(req.getAmount()));
                command.setInitiatedBy(req.getInitiatedBy());
                command.setOrderId(linkId);
                command.setUsername(credentials.getUsername());

                // Add transaction scheme placeholder if needed
                TransactionSchemeDetailsRequestDto scheme = new TransactionSchemeDetailsRequestDto();
                // TODO: fill required scheme fields
                command.setSchemeDetails(scheme);

                String uatUrl = credentials.getBaseUrlUat() + "/create-order";
                String prodUrl = credentials.getBaseUrlProd() + "/create-order";

                log.info("AES Admin panel order URL: {}", uatUrl);

                HttpEntity<PGCreateOrderCommand> requestEntity =
                        new HttpEntity<>(command, headers);

                ResponseEntity<CommonResponse<PgUnifiedOrderResponse>> response =
                        restTemplate.exchange(
                                uatUrl,
                                HttpMethod.POST,
                                requestEntity,
                                new ParameterizedTypeReference<CommonResponse<PgUnifiedOrderResponse>>() {}
                        );

                CommonResponse<PgUnifiedOrderResponse> responseBody = response.getBody();

                PgUnifiedOrderResponse pgResponse = responseBody.getData();

                commonFunctions.saveVendorLogs(
                        LocalDateTime.now(),
                        "AES order request",
                        vendor.getId(),
                        LocalDateTime.now(),
                        pgResponse.toString(),
                        null
                );

                PgTransactionMapping mapping = new PgTransactionMapping();
                mapping.setOrderId(linkId);
                mapping.setAmount(Double.valueOf(req.getAmount()));
                mapping.setStatus("PENDING");
                mapping.setInitiatedBy(req.getInitiatedBy());
                mapping.setSubProductId(req.getSubProductId());
                mapping.setVendorName(vendor.getVendorName());
                mapping.setLatitude(requestHeaderMetaDtoMeta.getLatitude());
                mapping.setLongitude(requestHeaderMetaDtoMeta.getLongitude());

                pgTransactionMappingRepository.save(mapping);

                return pgResponse;

            }

            HttpHeaders headers = buildPgHeaders(credentials);

            Map<String, Object> body =
                    buildPgRequestBody(req, credentials, subProduct, requestHeaderMetaDtoMeta);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    credentials.getBaseUrlProd() + "/paymentgatwayAPI/generateRequest",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map responseBody = response.getBody();

            log.info("IM PG Response | linkId={}, response={}", linkId, responseBody);

            if (response.getStatusCode().is2xxSuccessful()
                    && responseBody != null
                    && "success".equalsIgnoreCase(String.valueOf(responseBody.get("status")))) {

                String paymentLink = String.valueOf(responseBody.get("data"));
                String txRef = String.valueOf(responseBody.get("tx_ref"));

                PgTransactionMapping mapping = new PgTransactionMapping();
                mapping.setOrderId(linkId);
                mapping.setAmount(Double.valueOf(req.getAmount()));
                mapping.setStatus("PENDING");
                mapping.setInitiatedBy(req.getInitiatedBy());
                mapping.setSubProductId(req.getSubProductId());
                mapping.setVendorName(vendor.getVendorName());
                mapping.setLatitude(requestHeaderMetaDtoMeta.getLatitude());
                mapping.setLongitude(requestHeaderMetaDtoMeta.getLongitude());
                mapping.setClientIp(requestHeaderMetaDtoMeta.getIpAddress());
                mapping.setMedium(requestHeaderMetaDtoMeta.getMedium());
                mapping.setRequestDomainName(requestHeaderMetaDtoMeta.getRequestDomainName());

                // if these fields exist in your entity
                mapping.setVendorName("IM Payment Gateway");
                mapping.setTxnId(txRef);

                pgTransactionMappingRepository.save(mapping);

                return PgUnifiedOrderResponse.builder()
                        .vendor("IM PG")
                        .orderId(linkId)
                        .paymentLink(paymentLink)
                        .orderToken(null)
                        .status("INITIATED")
                        .build();
            }

            String errorMsg = responseBody != null
                    ? String.valueOf(responseBody.get("msg"))
                    : "Payment link creation failed";

            throw new RuntimeException(errorMsg);

        } catch (Exception e) {
            log.error("IM PG Payment Link API error", e);
            throw new RuntimeException("IM PG API Failed: " + e.getMessage());
        }

    }

     /* =====================================================
       BUILD PAYOUT HEADERS
       ===================================================== */

    private HttpHeaders buildPgHeaders(VendorCredentials creds) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer "+creds.getToken());
        log.info("<----------- Headers{} ------------>", headers);
        return headers;
    }

    private Map<String, Object> buildPgRequestBody(
            ImPgCreateOrderRequest req,
            VendorCredentials creds,
            SubProducts subProduct,
            RequestHeaderMetaDto requestHeaderMetaDto) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("geolocation",
                requestHeaderMetaDto.getLatitude() + "," + requestHeaderMetaDto.getLongitude());

        body.put("userid", creds.getUsername());
        body.put("clientip", requestHeaderMetaDto.getIpAddress());

        body.put("medium", requestHeaderMetaDto.getMedium() != null
                ? requestHeaderMetaDto.getMedium()
                : "rweb");

        body.put("holder_mobile_no", req.getHolderMobileNumber());
        body.put("card_holder_name", req.getCardHolderName());
        body.put("subproduct", subProduct.getSubProductTitle());
        body.put("amount", String.valueOf(req.getAmount()));
        body.put("paymentMode", req.getPaymentMode());

        log.info("<------------- IM PG Request Body {} ------------->", body);

        return body;
    }

    @Override
    public PgStatusResponse checkStatus(String orderId) {
        return null;
    }

    @Override
    @Transactional
    public void handleCallback(IMCallbackRequest imCallbackRequest, HttpServletRequest request) {

        log.info("NixaSoft Callback Started {}", imCallbackRequest.toString());
        log.info("Processing NixaSoft callback for orderId={}", imCallbackRequest.getTransactionReference());

        PgTransactionMapping pgTransactionMapping =
                pgTransactionMappingRepository
                        .findByTxnId(imCallbackRequest.getTransactionReference())
                        .orElseThrow(() ->
                                new RuntimeException("Transaction not found"));

        // Prevent duplicate success processing
        if ("SUCCESS".equalsIgnoreCase(pgTransactionMapping.getStatus())) {
            log.warn(
                    "Transaction already marked SUCCESS for orderId={}",
                    request.getRequestId()
            );
            return;
        }

        // Map the status and save
        String mappedStatus = StatusMapper.mapIMStatusToInternalStatus(imCallbackRequest.getStatus());
        pgTransactionMapping.setStatus(mappedStatus);
        pgTransactionMappingRepository.save(pgTransactionMapping);

        String subProductId = pgTransactionMapping.getSubProductId();

        SubProducts subProducts =
                subProductsRepository.findById(subProductId)
                        .orElseThrow(() -> {
                            log.error("---- [Callback] SubProduct NOT FOUND for subProductId: {}", subProductId);
                            return new RuntimeException("SubProduct not found for subProductId: " + subProductId);
                        });

        log.info("---- [Callback] SubProduct fetched successfully | subProductId={}, subProductCode={}",
                subProducts.getId(),
                subProducts.getSubProductCode());

        SchemeAllocation schemeAllocation =
                schemeAllocationRepository
                        .findByHierarchyIdAndSubProductCode(
                                pgTransactionMapping.getInitiatedBy(),
                                subProducts.getSubProductCode()
                        )
                        .orElseThrow(() -> new RuntimeException(
                                "SchemeAllocation not found | hierarchyId="
                                        + pgTransactionMapping.getInitiatedBy()
                                        + ", subProductCode="
                                        + subProducts.getSubProductCode()
                        ));

        String schemeCode = schemeAllocation.getSchemeCode();

        log.info("---- [Callback] SchemeAllocation fetched successfully | schemeCode={}", schemeCode);

        // Prepare base data
        Double txnAmt = pgTransactionMapping.getAmount();
        log.info("---- [Callback] Parsed transaction amount: {}", txnAmt);

        String operator = imCallbackRequest.getOperator();

        String operatorName = commonMethods.formatOperatorName(operator);

        log.info("---- [Callback] Using operatorName for DB lookup: {}", operatorName);

        List<SchemeRate> matchingSchemes =
                schemeRateRepository.findBySchemeCodeAndOperatorName(
                        schemeCode,
                        operatorName
                );

        if (matchingSchemes == null || matchingSchemes.isEmpty()) {

            log.error(
                    "---- [Callback] No matching scheme found for schemeCode={} and operatorName={}",
                    schemeCode,
                    operatorName
            );

            return;
        }

        log.info("---- [Callback] Scheme(s) found: {}", matchingSchemes.size());

        // Pick correct scheme by amount range or fixed value
        SchemeRate selectedScheme = matchingSchemes.stream()
                .filter(scheme ->
                        scheme.getSlabType() == SlabType.VARIABLE &&
                                txnAmt >= scheme.getSlabStart() && txnAmt <= scheme.getSlabEnd()
                )
                .findFirst()
                .orElse(matchingSchemes.get(0));
        log.info("---- [Callback] Selected scheme: {}", selectedScheme);

        // Fetch scheme, product & subproduct details
        log.debug("---- [Callback] Fetching scheme details for code: {}", selectedScheme.getSchemeCode());
        log.info("---- [Callback] Fetching scheme details for code: {}", selectedScheme.getSchemeCode());
        Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
                .orElseThrow(() -> new RuntimeException("Scheme Not Found for code: " + selectedScheme.getSchemeCode()));

        String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
        short productId = Short.parseShort(hashParts[0]);
        String subProductIdStr = hashParts[1];
        log.debug("---- [Callback] Extracted productId: {}, subProductId: {}", productId, subProductIdStr);
        log.info("---- [Callback] Extracted productId: {}, subProductId: {}", productId, subProductIdStr);

        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
                .orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + subProductIdStr));

        String orderId = pgTransactionMapping.getOrderId();

        String baseTranxId = orderId.startsWith("ORD") ? orderId.substring(3) : orderId;

        // Build DTO (common for all statuses)
        TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();

        dto.setBaseTranxId(baseTranxId);
        dto.setAmount(txnAmt);
        dto.setConfirmAmount(txnAmt);
        dto.setOperatorName(operatorName);
        dto.setOperatorCode(selectedScheme.getOperatorCode());
        dto.setSchemeRate(selectedScheme);
        dto.setTransactionType(product.getTransactionType());
        dto.setWalletType(subProduct.getWalletType());
        dto.setGstValue(String.valueOf(product.getSgst()));
        dto.setTdsRate(String.valueOf(product.getTds()));
        dto.setHierarchyId(pgTransactionMapping.getInitiatedBy());
        dto.setProductCode(product.getProductCode());
        dto.setSubProductCode(subProduct.getSubProductCode());
        dto.setIsCommission(product.getIsCommissions());
        dto.setHasCharge(product.getHasCharges());
        dto.setVendorTxnId(imCallbackRequest.getTxid());
        dto.setVendorName("IM Payment Gateway");
        dto.setMedium(pgTransactionMapping.getMedium());
        dto.setClientIp(pgTransactionMapping.getClientIp());
        dto.setRequestDomainName(pgTransactionMapping.getRequestDomainName());

        dto.setTransactionType("CREDIT");

        log.debug("---- [Callback] DTO prepared: {}", dto);
        log.info("---- [Callback] DTO prepared: {}", dto);

        String vendorStatus = imCallbackRequest.getStatus();

        String status = StatusMapper.mapIMStatusToInternalStatus(vendorStatus);

        // Forward to central transaction engine
        log.info("---- [Callback] Forwarding to Transaction Engine with Status: {}", vendorStatus);
        transactionServiceImpl.processTransaction(dto, status);

        log.info("Transaction updated successfully orderId={}, status={}", orderId, status);
    }


}
