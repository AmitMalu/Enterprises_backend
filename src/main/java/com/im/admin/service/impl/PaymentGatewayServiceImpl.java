package com.im.admin.service.impl;


import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.PaymentGatewayCardBinRequestDTO;
import com.im.admin.dto.RetailerCommissionResponseDTO;
import com.im.admin.dto.TokenResponseDTO;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.request.CreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.PaymentGatewayService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    @Autowired
    private CardTypeMasterRepository cardTypeMasterRepository;

    @Autowired
    private OperatorsRepository operatorsRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeAllocationRepository schemeAllocationRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private CommonMethods commonMethods;

    RestTemplate restTemplate = new RestTemplate();

    @Value("${runpaisa.token.username}")
    private String runpaisaUsername;

    @Value("${runpaisa.token.password}")
    private String runpaisaPassword;

    @Value("${runpaisa.base.url.prod}")
    private String baseUrlPROD;

    @Value("${runpaisa.client.id}")
    private String clientId;

    @Value("${runpaisa.create.order.url.prod}")
    private String orderUrl;

    @Override
    public String generateToken() {

        // Set headers only
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("username", runpaisaUsername);
        headers.set("password", runpaisaPassword);
        headers.set("client_id", clientId);

        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

        String url = baseUrlPROD + "/token";
        log.info("Sending token request to URL: {}", url);

        ResponseEntity<TokenResponseDTO> response = restTemplate.postForEntity(
                url,
                requestEntity,
                TokenResponseDTO.class
        );

        TokenResponseDTO tokenResponse = response.getBody();
        log.info("Bearer token response: {}", tokenResponse);

        if (tokenResponse != null && tokenResponse.getData() != null) {
            return tokenResponse.getData().getToken();
        } else {
            throw new RuntimeException("Failed to get token from Runpaisa");
        }
    }

    @Override
    public CommonResponse<RetailerCommissionResponseDTO> fetchRetailerCommission(PaymentGatewayCardBinRequestDTO requestDTO) {

        // 1. Validate Card BIN
        CardTypeMaster cardTypeMaster = cardTypeMasterRepository
                .findByCardBinNo(requestDTO.getCardBinNo())
                .orElseThrow(() -> new RuntimeException("Invalid Card BIN number"));

        String cardType = cardTypeMaster.getCardType();
        String normalizedCardType = cardType.replaceAll("[^a-zA-Z]", "").toLowerCase();
        String operatorCode = normalizedCardType.contains("creditcard") ? "CC" : "DC";

        // 2. Find Operator
        Operators operator = operatorsRepository
                .findByProductCodeAndSubProductCodeAndOperatorCode(
                        requestDTO.getProductCode(),
                        requestDTO.getSubProductCode(),
                        operatorCode
                )
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        // 3. Get SchemeAllocation by SchemeCode + Product + SubProduct + HierarchyId
        SchemeAllocation allocation = schemeAllocationRepository
                .findBySchemeCodeAndProductCodeAndSubProductCodeAndHierarchyId(
                        requestDTO.getSchemeCode(),      // <-- NEW: Scheme code from request
                        requestDTO.getProductCode(),
                        requestDTO.getSubProductCode(),
                        requestDTO.getAgentId()
                )
                .orElseThrow(() -> new RuntimeException("No scheme allocated to this retailer"));

        // 4. Fetch Scheme
        Scheme scheme = schemeRepository
                .findBySchemeCode(allocation.getSchemeCode())
                .orElseThrow(() -> new RuntimeException("Scheme not found"));

        // 5. Fetch SchemeRate using schemeCode + operatorCode
        SchemeRate schemeRate = schemeRateRepository
                .findBySchemeCodeAndOperatorCode(
                        scheme.getSchemeCode(),
                        operator.getOperatorCode()
                );

        if (schemeRate == null) {
            throw new RuntimeException("Scheme rate not found for operator");
        }

        RetailerCommissionResponseDTO dto = new RetailerCommissionResponseDTO(
                cardType,
                operator.getOperatorCode(),
                scheme.getSchemeCode(),
                schemeRate.getRetailer(),
                schemeRate.getRateType()
        );

        return new CommonResponse<>(200, "Retailer commission fetched successfully", dto);
    }

    @Override
    public ResponseEntity<?> createOrder(CreateOrderRequest req) {
        try {

            //Step 1: Generate token
            String token = generateToken();

            // Step 2: Build multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("client_id", clientId);
            headers.set("token", token);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("callbackurl", req.getCallbackUrl());
            String baseTransactionId = commonMethods.generateBaseTransactionId();
            body.add("order_id", "ORD" + baseTransactionId);
            body.add("amount", String.valueOf(req.getAmount()));

            if (req.getMerchantRef() != null) {
                body.add("merc_unq_ref", req.getMerchantRef());
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            log.info("CreateOrder Request: headers={}, body={}", headers, body);
            ResponseEntity<String> response = restTemplate.postForEntity(orderUrl, requestEntity, String.class);

            log.info("Run paisa create order response: {}", response.getBody());
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            log.error("Error while creating order: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Order creation failed");
        }
    }
}
