package com.scs.admin.service.impl;

import com.scs.admin.commons.LoggableVendor;
import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.dto.TransactionSchemeDetailsRequestDto;
import com.scs.admin.entities.*;
import com.scs.admin.entities.enums.SlabType;
import com.scs.admin.entities.product.Products;
import com.scs.admin.entities.vendor.Vendors;
import com.scs.admin.repositories.*;
import com.scs.admin.request.NixaSoftCallbackRequest;
import com.scs.admin.request.RazorPayCreateOrderRequest;
import com.scs.admin.service.RazorPayPGService;
import com.scs.admin.util.CommonMethods;
import com.razorpay.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author SHUBHAM KHOPADE
 */

@Service
@Slf4j
@LoggableVendor
public class RazorPayPGServiceImpl implements RazorPayPGService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private PgTransactionMappingRepository pgTransactionMappingRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private SchemeAllocationRepository schemeAllocationRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private SchemeRepository schemeRepository;


    @Override
    public String createOrder(RazorPayCreateOrderRequest req, RequestHeaderMetaDto requestHeaderMetaDto) {

        log.info("Starting Razorpay Create Order for subProductId={}, initiatedBy={}, amount={}",
                req.getSubProductId(), req.getInitiatedBy(), req.getAmount());

        try {

            Optional<Retailer> retailer = retailerRepository.findByRetailerId(req.getInitiatedBy());

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

            String keyId = credentials.getClientId();
            String keySecret = credentials.getClientSecret();

            // ===== 2. Generate Internal Order ID =====
            String orderId = "ORD" + commonMethods.generateBaseTransactionId();

            // ===== 3. Create Razorpay Client =====
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

            // ===== 4. Prepare Request =====
            JSONObject request = new JSONObject();

            request.put("amount", (int) Math.round(req.getAmount() * 100));
            request.put("currency", "INR");
            request.put("reference_id", orderId);
            request.put("description", req.getDescription());

            // Expiry: 1 hour
            request.put("expire_by", Instant.now().getEpochSecond() + 3600);

            // Callback (UI redirect only)
            request.put("callback_url", credentials.getCallbackUrl());
            request.put("callback_method", "get");

            // Customer
            JSONObject customer = new JSONObject();
            customer.put("name", req.getCustomer().getName());
            customer.put("contact", req.getCustomer().getContact());
            customer.put("email", retailer.get().getEmailId());
            request.put("customer", customer);

            // Notify
            JSONObject notify = new JSONObject();
            notify.put("sms", true);
            notify.put("email", true);
            request.put("notify", notify);

            // Notes (IMPORTANT for tracking)
            JSONObject notes = new JSONObject();
            notes.put("internal_order_id", orderId);
            notes.put("sub_product_id", req.getSubProductId());
            request.put("notes", notes);

            // ===== 5. Call Razorpay API =====
            PaymentLink paymentLink = razorpay.paymentLink.create(request);

            String status = paymentLink.get("status");          // created
            String paymentUrl = paymentLink.get("short_url");   // actual payment link
            String plinkId = paymentLink.get("id");             // plink_xxx

            log.info("Razorpay response status={}, plinkId={}", status, plinkId);

            // ===== 6. Save Transaction =====
            PgTransactionMapping transaction = new PgTransactionMapping();
            transaction.setOrderId(orderId);
            transaction.setAmount(req.getAmount().doubleValue());
            transaction.setInitiatedBy(req.getInitiatedBy());
            transaction.setSubProductId(req.getSubProductId());
            transaction.setStatus(status);
            transaction.setVendorName("RazorPay PG");
            transaction.setVendorSessionId(plinkId);
            transaction.setLatitude(requestHeaderMetaDto.getLatitude());
            transaction.setLongitude(requestHeaderMetaDto.getLongitude());
            transaction.setCreatedBy(retailer.get().getMobileNumber());

            pgTransactionMappingRepository.save(transaction);

            // ===== 7. Return Payment URL =====
            if ("created".equalsIgnoreCase(status)) {

                log.info("Payment link generated successfully for orderId={}", orderId);
                return paymentUrl;

            } else {

                log.error("Failed to create Razorpay payment link for orderId={}", orderId);
                throw new RuntimeException("Failed to create payment link");
            }

        } catch (RazorpayException e) {
            log.error("Razorpay API error", e);
            throw new RuntimeException("Razorpay API Failed");
        }
    }

    @Override
    @Transactional
    public void processCallback(String paymentId,
                                String plinkId,
                                String orderId,
                                String status,
                                String signature) {

        log.info("===== [RAZORPAY CALLBACK START] =====");
        log.info("Incoming callback: paymentId={}, plinkId={}, orderId={}, status={}",
                paymentId, plinkId, orderId, status);

        try {

            // ===== 1. FETCH TRANSACTION =====
            PgTransactionMapping txn = pgTransactionMappingRepository
                    .findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found for orderId=" + orderId));

            log.info("Transaction found: orderId={}, currentStatus={}", txn.getOrderId(), txn.getStatus());

            // ===== 2. IDEMPOTENCY CHECK =====
            if ("SUCCESS".equalsIgnoreCase(txn.getStatus())) {
                log.warn("Transaction already SUCCESS. Skipping processing for orderId={}", orderId);
                return;
            }

            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(txn.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials = vendorCredentialsRepository.findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId()).orElseThrow(() -> new RuntimeException("No credentials found"));

            log.info("Vendor credentials fetched successfully for vendorId={}, vendorName={}", vendor.getId(), vendor.getVendorName());

            RazorpayClient razorpay = new RazorpayClient(
                    credentials.getClientId().trim(),
                    credentials.getClientSecret().trim()
            );

            log.info("Razorpay client initialized");

            // ===== 4. FETCH PAYMENT FROM RAZORPAY =====
            Payment payment;

            try {
                payment = razorpay.payments.fetch(paymentId);
                log.info("Payment fetched from Razorpay: paymentId={}", paymentId);
            } catch (Exception ex) {
                log.error("Failed to fetch payment from Razorpay: paymentId={}", paymentId, ex);
                throw new RuntimeException("Payment verification failed");
            }

            String razorpayStatus = payment.get("status"); // captured / failed
            Double amount = txn.getAmount();
            JSONObject paymentJson = new JSONObject(payment.toString());

            String method = paymentJson.optString("method", null);

            String cardType = null;
            String cardNetwork = null;
            String entity = method;

            if ("card".equalsIgnoreCase(method)) {

                JSONObject card = payment.toJson().getJSONObject("card");

                cardType = card.optString("type", null);
                cardNetwork = card.optString("network", null);

            } else if ("upi".equalsIgnoreCase(method)) {

                cardType = "upi";
                cardNetwork = paymentJson.optString("vpa", "UPI");

            } else if ("netbanking".equalsIgnoreCase(method)) {

                cardType = "netbanking";
                cardNetwork = paymentJson.optString("bank", "BANK");

            } else if ("wallet".equalsIgnoreCase(method)) {

                cardType = "wallet";
                cardNetwork = paymentJson.optString("wallet", "WALLET");
            }

            log.info("Extracted payment details: method={}, cardType={}, cardNetwork={}",
                    method, cardType, cardNetwork);

            log.info("Razorpay payment status={}, amount={}", razorpayStatus, amount);

            // ===== 5. DETERMINE FINAL STATUS =====
            String finalStatus;

            if ("captured".equalsIgnoreCase(razorpayStatus)) {
                finalStatus = "SUCCESS";
            } else if ("failed".equalsIgnoreCase(razorpayStatus)) {
                finalStatus = "FAILED";
            } else {
                finalStatus = "PENDING";
            }

            log.info("Final mapped status={}", finalStatus);

            // ===== 6. UPDATE TXN BASIC INFO =====
            txn.setVendorSessionId(plinkId);   // keep plinkId
            txn.setTxnId(paymentId);           // store paymentId separately
            txn.setStatus(finalStatus);

            // ===== 7. BUILD DTO FOR ENGINE =====
            TransactionSchemeDetailsRequestDto dto = buildTransactionDto(paymentId, txn, amount, cardType, cardNetwork, entity);

            log.info("Transaction DTO prepared for orderId={}", orderId);

            // ===== 8. CALL TRANSACTION ENGINE =====
            transactionEngineServiceImpl.processTransaction(dto, finalStatus);

            log.info("Transaction engine processed for orderId={}, status={}", orderId, finalStatus);

            // ===== 9. SAVE =====
            pgTransactionMappingRepository.save(txn);

            log.info("Transaction updated successfully for orderId={}", orderId);
            log.info("===== [RAZORPAY CALLBACK END SUCCESS] =====");

        } catch (Exception e) {
            log.error("===== [RAZORPAY CALLBACK FAILED] orderId={} =====", orderId, e);
            throw new RuntimeException("Callback processing failed");
        }
    }

    private TransactionSchemeDetailsRequestDto buildTransactionDto(String paymentId, PgTransactionMapping txn, Double amount, String cardType, String cardNetwork, String entity) {

        log.info("Building Transaction DTO for orderId={}", txn.getOrderId());

        SubProducts subProduct = subProductsRepository.findById(txn.getSubProductId())
                .orElseThrow(() -> new RuntimeException("SubProduct not found"));

        SchemeAllocation schemeAllocation = schemeAllocationRepository
                .findByHierarchyIdAndSubProductCode(txn.getInitiatedBy(), subProduct.getSubProductCode())
                .orElseThrow(() -> new RuntimeException("SchemeAllocation not found"));

        String schemeCode = schemeAllocation.getSchemeCode();

        String operatorName = toDbOperatorName(cardType, entity, cardNetwork);

        List<SchemeRate> schemes = schemeRateRepository
                .findBySchemeCodeAndOperatorName(schemeCode, operatorName);

        SchemeRate selectedScheme = schemes.get(0);

        Scheme scheme = schemeRepository.findBySchemeCode(schemeCode)
                .orElseThrow(() -> new RuntimeException("Scheme not found"));

        String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
        short productId = Short.parseShort(hashParts[0]);

        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();

        dto.setBaseTranxId(txn.getOrderId().replace("ORD", ""));
        dto.setAmount(amount);
        dto.setConfirmAmount(amount);
        dto.setOperatorName(operatorName);
        dto.setSchemeRate(selectedScheme);
        dto.setTransactionType(product.getTransactionType());
        dto.setWalletType(subProduct.getWalletType());
        dto.setGstValue(String.valueOf(product.getSgst()));
        dto.setTdsRate(String.valueOf(product.getTds()));
        dto.setIsCommission(product.getIsCommissions());
        dto.setHasCharge(product.getHasCharges());
        dto.setHierarchyId(txn.getInitiatedBy());
        dto.setProductCode(product.getProductCode());
        dto.setSubProductCode(subProduct.getSubProductCode());
        dto.setLatitude(txn.getLatitude());
        dto.setLongitude(txn.getLongitude());
        dto.setVendorName("RAZORPAY Payment Gateway");
        dto.setVendorTxnId(paymentId);
        dto.setMedium(txn.getMedium());
        dto.setClientIp(txn.getClientIp());
        dto.setRequestDomainName(txn.getRequestDomainName());
        dto.setMobileNumber(txn.getCreatedBy());

        dto.setTransactionType("CREDIT");

        return dto;
    }

    private String toDbOperatorName(String cardType, String entity, String cardNetwork) {
        log.info("<----------------------- Card Category: {}, PG Partner: {}", cardType, cardNetwork);

        //debit, card, visa
        return Stream.of(cardType, entity, cardNetwork)
                .filter(s -> s != null && !s.isBlank() && !s.equalsIgnoreCase("NA") && !s.equalsIgnoreCase("N/A"))
                .map(s -> Arrays.stream(s.split("_"))
                        .filter(part -> !part.isBlank())
                        .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                        .collect(Collectors.joining("_")))
                .collect(Collectors.joining("_"));
    }

}
