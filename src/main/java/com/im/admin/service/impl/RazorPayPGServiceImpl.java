package com.im.admin.service.impl;

import com.im.admin.entities.PgTransactionMapping;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.PgTransactionMappingRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.RazorPayCreateOrderRequest;
import com.im.admin.service.RazorPayPGService;
import com.im.admin.util.CommonMethods;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * @author SHUBHAM KHOPADE
 */

@Service
@Slf4j
public class RazorPayPGServiceImpl implements RazorPayPGService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private PgTransactionMappingRepository pgTransactionMappingRepository;


    @Override
    public String createOrder(RazorPayCreateOrderRequest req, Double latitude, Double longitude) {

        log.info("Starting Razorpay Create Order for subProductId={}, initiatedBy={}, amount={}",
                req.getSubProductId(), req.getInitiatedBy(), req.getAmount());

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
            customer.put("email", req.getCustomer().getEmail());
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
            transaction.setLatitude(latitude);
            transaction.setLongitude(longitude);

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

}
