package com.im.admin.service.impl;

import com.im.admin.dto.BeneficiaryVerifyRequestDto;
import com.im.admin.dto.BeneficiaryVerifyResponseDto;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.service.PayoutBeneficiaryService;
import com.im.admin.service.PayoutBeneficiaryVerifier;
import com.im.admin.service.PayoutVendorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.StringReader;


@Service
@Slf4j
public class UnisusPayoutServiceImpl implements PayoutVendorService, PayoutBeneficiaryVerifier {

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
            WalletType walletType
    ) {

        try {

            String ntdReq = buildNTDRequest(req, orderId);

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

    private String buildNTDRequest(SendPayoutRequest req, String refNo) {

        return "<NTDREQ>" +
                "<REQTYPE>SM</REQTYPE>" +
                "<REFNO>" + refNo + "</REFNO>" +
                "<CSTNO>9999999999</CSTNO>" +        // can come from creds
                "<CSTNM>" + req.getBeneficiaryName() + "</CSTNM>" +
                "<BANKID>1</BANKID>" +               // ideally from bank mapping
                "<ACCNO>" + req.getAccountNumber() + "</ACCNO>" +
                "<ACCNM>" + req.getBeneficiaryName() + "</ACCNM>" +
                "<IFSC>" + req.getIfscCode() + "</IFSC>" +
                "<AMT>" + req.getAmount() + "</AMT>" +
                "<TRNTYPE>" + mapTransferType(req.getPaymentMode()) + "</TRNTYPE>" +
                "<CHN>WEB</CHN>" +
                "<AG_LAT></AG_LAT>" +
                "<AG_LONG></AG_LONG>" +
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

    private String generateChecksum(String input) {

        try {

            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Checksum generation failed", e);
        }
    }

    private String buildSoapRequest(String ntdReq, VendorCredentials creds) {

        String checksum = generateChecksum(ntdReq);

        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +

                "<soap:Header>" +
                "<DMRAuthHeader xmlns=\"http://tempuri.org/\">" +
                "<MobileNo>" + creds.getUsername() + "</MobileNo>" +
                "<APIKey>" + creds.getPassword() + "</APIKey>" +
                "<ResponseType>XML</ResponseType>" +
                "<Checksum>" + checksum + "</Checksum>" +
                "<Version>1.0</Version>" +
                "<AgentCode>" + creds.getToken() + "</AgentCode>" +
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
}
