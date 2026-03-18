package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.BeneficiaryVerifyRequestDto;
import com.im.admin.dto.BeneficiaryVerifyResponseDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.PayoutStatusResponse;
import com.im.admin.service.PayoutBeneficiaryVerifier;
import com.im.admin.service.PayoutCallbackHandler;
import com.im.admin.service.PayoutVendorService;
import com.im.admin.service.VendorStatusChecker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.StringReader;


@Service
@Slf4j
public class UnisusPayoutServiceImpl implements PayoutVendorService, PayoutBeneficiaryVerifier, PayoutCallbackHandler, VendorStatusChecker {

    @Autowired
    private NetworkBankMasterRepository bankMasterRepository;

    @Autowired
    private PayoutPendingTransactionRepository payoutPendingTransactionRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate = new RestTemplate();


    @Override
    public String getVendorCode() {
        return "UnisusPay Payout";
    }

    @Override
    public PayoutResponseDto sendPayout(
            SendPayoutRequest req,
            VendorCredentials creds,
            String orderId,
            SubProducts subProducts,
            WalletType walletType,
            Double latitude,
            Double longitude
    ) {

        try {

            String ntdReq = buildNTDRequest(req, orderId, creds);

            String soapRequest = buildSoapRequest(ntdReq, creds);

            log.info("UNISUS REQUEST {}", soapRequest);

            String response = callApi(soapRequest, creds);

            log.info("UNISUS RESPONSE {}", response);

            return parseResponse(response, orderId);

        } catch (Exception e) {

            log.error("Unisus payout failed", e);

            return PayoutResponseDto.builder()
                    .orderId(orderId)
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
        }
    }

    @Override
    public BeneficiaryVerifyResponseDto verifyBeneficiary(
            BeneficiaryVerifyRequestDto req,
            VendorCredentials credentials) {

        try {

            String xmlRequest =
                    "<NTDREQ>" +
                            "<REQTYPE>BAV</REQTYPE>" +
                            "<REFNO>" + "BAV" + UUID.randomUUID() + "</REFNO>" +
                            "<CSTNO>" + req.getCustomerMobile() + "</CSTNO>" +
                            "<CSTNM>" + req.getCustomerName() + "</CSTNM>" +
                            "<BANKID>" + req.getBankId() + "</BANKID>" +
                            "<ACCNO>" + req.getAccountNumber() + "</ACCNO>" +
                            "<IFSC>" + req.getIfsc() + "</IFSC>" +
                            "</NTDREQ>";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);

            HttpEntity<String> entity = new HttpEntity<>(xmlRequest, headers);

            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            credentials.getBaseUrlUat(),
                            entity,
                            String.class
                    );

            log.info("Unisus Beneficiary Verify Response: {}", response.getBody());

            // parse response XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(
                    new InputSource(new StringReader(Objects.requireNonNull(response.getBody())))
            );

            String statusCode =
                    doc.getElementsByTagName("STATUSCODE").item(0).getTextContent();

            String statusMsg =
                    doc.getElementsByTagName("STATUSMSG").item(0).getTextContent();

            String name =
                    doc.getElementsByTagName("NAME").item(0).getTextContent();

            String verifyId =
                    doc.getElementsByTagName("VERIFYID").item(0).getTextContent();

            return BeneficiaryVerifyResponseDto.builder()
                    .beneficiaryName(name)
                    .verifyId(verifyId)
                    .status(statusCode.equals("0") ? "SUCCESS" : "FAILED")
                    .message(statusMsg)
                    .build();

        } catch (Exception e) {

            log.error("Unisus Beneficiary Verify Failed", e);

            return BeneficiaryVerifyResponseDto.builder()
                    .status("FAILED")
                    .message("Verification failed")
                    .build();
        }
    }

    private String buildNTDRequest(SendPayoutRequest req, String refNo, VendorCredentials creds) {

        NetworkBankMaster bank = bankMasterRepository
                .findByUnisusMIfcsCodeIgnoreCase(req.getIfscCode().trim())
                .orElseThrow(() -> new NoSuchElementException("No bank found with IFSC: " + req.getIfscCode()));

        return "<NTDREQ>" +
                "<REQTYPE>SM</REQTYPE>" +
                "<REFNO>" + refNo + "</REFNO>" +
                "<CSTNO>" + creds.getUsername() + "</CSTNO>" +
                "<CSTNM>" + req.getBeneficiaryName() + "</CSTNM>" +
                "<BANKID>" + bank.getUnisusBankId() + "</BANKID>" +
                "<ACCNO>" + req.getAccountNumber() + "</ACCNO>" +
                "<ACCNM>" + req.getBeneficiaryName() + "</ACCNM>" +
                "<IFSC>" + req.getIfscCode() + "</IFSC>" +
                "<AMT>" + req.getAmount() + "</AMT>" +
                "<TRNTYPE>" + mapTransferType(req.getPaymentMode()) + "</TRNTYPE>" +
                "<CHN>" + req.getChannelType().toUpperCase() + "</CHN>" +
                "<AG_LAT>" + req.getLatitude() + "</AG_LAT>" +
                "<AG_LONG>" + req.getLongitude() + "</AG_LONG>" +
                "</NTDREQ>";
    }

    private String mapTransferType(String mode) {

        if (mode == null) return "1";

        return switch (mode.toUpperCase()) {
            case "IMPS" -> "1";
            case "NEFT" -> "2";
            case "RTGS" -> "3";
            default -> "1";
        };
    }

    private String generateChecksum(String data, String apiKey) {

        try {

            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");

            mac.init(secretKey);

            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();

            for (byte b : rawHmac) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString().toUpperCase();

        } catch (Exception e) {
            throw new RuntimeException("Checksum generation failed", e);
        }
    }

    private String buildSoapRequest(String ntdReq, VendorCredentials creds) {

        String checksum = generateChecksum(ntdReq, creds.getClientId());

        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +

                "<soap:Header>" +
                "<DMRAuthHeader xmlns=\"http://tempuri.org/\">" +
                "<MobileNo>" + creds.getUsername() + "</MobileNo>" +
                "<APIKey>" + creds.getPassword() + "</APIKey>" +
                "<ResponseType>JSON</ResponseType>" +
                "<Checksum>" + checksum + "</Checksum>" +
                "<Version>1.0</Version>" +
                "<AgentCode>" + "RT01" + "</AgentCode>" +
                "</DMRAuthHeader>" +
                "</soap:Header>" +

                "<soap:Body>" +
                "<SendMoney xmlns=\"http://tempuri.org/\">" +
                "<sRequest><![CDATA[" + ntdReq + "]]></sRequest>" +
                "</SendMoney>" +
                "</soap:Body>" +

                "</soap:Envelope>";
    }

    private String callApi(String soapRequest, VendorCredentials creds) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.add("SOAPAction", "http://tempuri.org/SendMoney");

        HttpEntity<String> entity = new HttpEntity<>(soapRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                creds.getBaseUrlUat(),
                HttpMethod.POST,
                entity,
                String.class
        );

        return response.getBody();
    }

    private PayoutResponseDto parseResponse(String response, String orderId) {

        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(
                    new InputSource(new StringReader(response))
            );

            Node resultNode = doc.getElementsByTagName("SendMoneyResult").item(0);

            String innerXml = StringEscapeUtils.unescapeXml(resultNode.getTextContent());

            Document innerDoc = builder.parse(
                    new InputSource(new StringReader(innerXml))
            );

            String status = innerDoc.getElementsByTagName("TRNSTATUS").item(0).getTextContent();
            String message = innerDoc.getElementsByTagName("TRNSTATUSDESC").item(0).getTextContent();

            return PayoutResponseDto.builder()
                    .orderId(orderId)
                    .status(status.equals("0") ? "PENDING" : "FAILED")
                    .message(message)
//                    .vendorTxnId(
//                            innerDoc.getElementsByTagName("TRNID").item(0).getTextContent()
//                    )
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Unisus response", e);
        }
    }

    @Override
    public String handleCallback(Map<String, String> payload, HttpServletRequest request)
            throws JsonProcessingException {

        String clientRefNo = payload.get("ClientRefNo");
        String status = payload.get("Status");
        String statusMsg = payload.get("StatusMsg");
        String trnId = payload.get("TrnID");
        String bankRefNo = payload.get("BankRefNo");
        String ttcAmount = payload.get("TTC");

        PayoutPendingTransactions pendingTxn = payoutPendingTransactionRepository.findByTransactionId(trnId)
                .orElseThrow(() -> new RuntimeException("Pending transaction not found for order ID: " + trnId));

        log.info("[1.1] PendingTxn fetched successfully: ID={}, SchemeCode={}, TxnType={}, Amount={}, Status={}",
                pendingTxn.getId(), pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(),
                pendingTxn.getAmount(), pendingTxn.getStatus());

        Double txnAmt = Double.valueOf(String.valueOf(pendingTxn.getAmount()));

        // Fetch matching SchemeRate
        log.info("[2] Searching SchemeRate for schemeCode={}, operatorName={}, amount={}",
                pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(), txnAmt);

        SchemeRate selectedScheme = schemeRateRepository
                .findSchemeRateByCodeAndOperatorAndAmount(
                        pendingTxn.getSchemeCode(),
                        pendingTxn.getTransactionType(),
                        txnAmt)
                .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + txnAmt));

        log.info("[2.1] Found SchemeRate: ID={}, RateType={}, SlabType={}, Gross={}, RetailerRate={}",
                selectedScheme.getId(), selectedScheme.getRateType(), selectedScheme.getSlabType(),
                selectedScheme.getGross(), selectedScheme.getRetailer());

        // Fetch scheme entity
        log.info("[3] Fetching Scheme entity for SchemeCode={}", selectedScheme.getSchemeCode());
        Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
                .orElseThrow(() -> new RuntimeException("Scheme Not Found with code: " + selectedScheme.getSchemeCode()));

        log.info("[3.1] Scheme fetched successfully: HashCode={}", scheme.getSchemeCombinationHashCode());

        // Parse scheme combination hash code
        String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
        if (hashParts.length < 2) {
            throw new RuntimeException("Invalid schemeCombinationHashCode format: " + scheme.getSchemeCombinationHashCode());
        }

        short productId = Short.parseShort(hashParts[0]);
        String subProductIdStr = hashParts[1];

        log.info("[4] Extracted ProductID={}, SubProductID={} from SchemeCombinationHashCode", productId, subProductIdStr);

        // Fetch product and subproduct
        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
                .orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + subProductIdStr));

        log.info("[4.1] Product/SubProduct fetched successfully: Product={}, SubProduct={}",
                product.getName(), subProduct.getSubProductTitle());

        // Prepare DTO (common for all statuses)
        log.info("[5] Constructing TransactionSchemeDetailsRequestDto...");
        TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();

        String baseTxnId = trnId.startsWith("TXN") ? trnId.substring(3) : trnId;

        dto.setBaseTranxId(baseTxnId);
        dto.setAmount(txnAmt);
        dto.setConfirmAmount(txnAmt);
        dto.setOperatorName(pendingTxn.getTransactionType());
        dto.setOperatorCode(selectedScheme.getOperatorCode());
        dto.setSchemeRate(selectedScheme);
        dto.setTransactionType(product.getTransactionType());
        dto.setWalletType(subProduct.getWalletType());
        dto.setGstValue(String.valueOf(product.getSgst()));
        dto.setTdsRate(String.valueOf(product.getTds()));
        dto.setProductCode(product.getProductCode());
        dto.setSubProductCode(subProduct.getSubProductCode());
        dto.setIsCommission(product.getIsCommissions());
        dto.setHasCharge(product.getHasCharges());
        dto.setHierarchyId(pendingTxn.getRetailerId());

        log.info("[6] DTO ready for transaction processing: {}", dto);

        statusMsg = switch (status) {
            case "1" -> "SUCCESS";
            case "2", "3" -> "FAILED";
            case "4", "6" -> "PENDING";
            default -> "UNKNOWN";
        };

        // Process transaction for any status (SUCCESS, FAILED, PENDING, etc.)
        transactionEngineServiceImpl.processTransaction(dto, statusMsg);
        log.info("[7] Transaction processed successfully for ORDER_ID: {}, STATUS={}",
                trnId, status);

        log.info("Vendor Callback Received: {}", payload);

        return "SUCCESS";
    }

    @Override
    public PayoutStatusResponse checkPayoutStatus(String orderId, VendorCredentials credentials) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestXml = null;
        String responseXml = null;
        String vendorId = credentials.getVendor().getVendorCode();

        try {

            String ntdReq = "<NTDREQ><REQTYPE>GTS</REQTYPE><REFNO>" + orderId + "</REFNO></NTDREQ>";

            String checksum = generateChecksum(ntdReq, credentials.getClientId());

            requestXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +

                    "<soap:Header>" +
                    "<DMRAuthHeader xmlns=\"http://tempuri.org/\">" +
                    "<MobileNo>" + credentials.getUsername() + "</MobileNo>" +
                    "<APIKey>" + credentials.getPassword() + "</APIKey>" +
                    "<ResponseType>JSON</ResponseType>" +
                    "<Checksum>" + checksum + "</Checksum>" +
                    "<Version>1.0</Version>" +
                    "<AgentCode>RT01</AgentCode>" +
                    "</DMRAuthHeader>" +
                    "</soap:Header>" +

                    "<soap:Body>" +
                    "<GetTransactionStatus xmlns=\"http://tempuri.org/\">" +
                    "<sRequest>&lt;NTDREQ&gt;&lt;REQTYPE&gt;GTS&lt;/REQTYPE&gt;&lt;REFNO&gt;" + orderId + "&lt;/REFNO&gt;&lt;/NTDREQ&gt;</sRequest>" +
                    "</GetTransactionStatus>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);
            headers.set("SOAPAction", "http://tempuri.org/GetTransactionStatus");

            HttpEntity<String> entity = new HttpEntity<>(requestXml, headers);

            log.info("[UNISUS] Checking payout status for REFNO={} ", orderId);

            ResponseEntity<String> response = restTemplate.exchange(
                    credentials.getBaseUrlProd(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            responseXml = response.getBody();

            log.info("[UNISUS] Raw SOAP response: {}", responseXml);

            // Extract JSON inside SOAP
            String json = responseXml.substring(
                    responseXml.indexOf("{"),
                    responseXml.lastIndexOf("}") + 1
            );

            JsonNode node = objectMapper.readTree(json).get("NTDRESP");

            String status = node.get("TRNSTATUS").asText();
            String message = node.get("TRNSTATUSDESC").asText();

            return new PayoutStatusResponse(orderId, status, message);

        } catch (Exception e) {

            responseXml = e.getMessage();

            log.error("[UNISUS] Error checking payout status for REFNO={}", orderId, e);

            return new PayoutStatusResponse(orderId, "FAILED", "Error during status check");

        } finally {

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestXml,
                    vendorId,
                    LocalDateTime.now(),
                    responseXml,
                    null
            );
        }
    }
}
