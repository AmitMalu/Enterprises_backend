package com.im.admin.service.impl;

import com.im.admin.entities.RechargeTransaction;
import com.im.admin.entities.Retailer;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.repositories.RechargeTransactionRepository;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.request.RechargeRequest;
import com.im.admin.response.RechargeResponse;
import com.im.admin.service.RechargeVendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CellmoneyRechargeServiceImpl implements RechargeVendorService {

    private static final String SMS_PIN = "5518";

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private RechargeTransactionRepository rechargeTransactionRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String getVendorCode() {
        return "Cellmoney Recharge";
    }


    @Override
    public RechargeResponse recharge(RechargeRequest request,
                                     VendorCredentials credentials) {

        Retailer retailer = retailerRepository.findById(request.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        //  Generate transaction reference
        String transactionRef = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        //  Build Message
        String message = request.getOperatorCode() + "$"
                + request.getCustomerNumber() + "$"
                + request.getAmount() + "$"
                + SMS_PIN + "$"
                + request.getRechargeType() + "$"
                + transactionRef;

        // Build URL
        String url = UriComponentsBuilder
                .fromHttpUrl(credentials.getBaseUrlUat())
                .queryParam("UserID", credentials.getUsername())
                .queryParam("Password", credentials.getPassword())
                .queryParam("MobileNo", credentials.getUsername())
                .queryParam("Message", message)
                .toUriString();

        // Save transaction as PENDING before calling vendor
        RechargeTransaction transaction = RechargeTransaction.builder()
                .transactionRef(transactionRef)
                .operatorCode(request.getOperatorCode())
                .customerNumber(request.getCustomerNumber())
                .amount(request.getAmount())
                .rechargeType(request.getRechargeType())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .retailer(retailer)
                .vendor(credentials.getVendor())
                .build();

        rechargeTransactionRepository.save(transaction);

        // Call Vendor API
        String vendorResponse = restTemplate.getForObject(url, String.class);

        transaction.setVendorResponse(vendorResponse);

// Split response
        String[] parts = vendorResponse.split("=");

        int statusCode = Integer.parseInt(parts[0]);
        String statusMessage = parts.length > 1 ? parts[1] : "Unknown Response";

// If vendor rejected request
        if (statusCode != 0) {

            transaction.setStatus("FAILED");
            rechargeTransactionRepository.save(transaction);

            return RechargeResponse.builder()
                    .transactionRef(transactionRef)
                    .status("FAILED")
                    .message(statusMessage)
                    .build();
        }

// If vendor accepted request
        transaction.setStatus("PENDING");
        rechargeTransactionRepository.save(transaction);

        return RechargeResponse.builder()
                .transactionRef(transactionRef)
                .status("PENDING")
                .message(statusMessage)   // show vendor message
                .build();
    }

    @Override
    public String checkBalance(RechargeRequest request, VendorCredentials credentials) {


        Retailer retailer = retailerRepository.findById(request.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        // 🔹 Build Message
        String message = "BAL$" + SMS_PIN;

        // 🔹 Build URL
        String url = UriComponentsBuilder
                .fromHttpUrl(credentials.getBaseUrlUat())
                .queryParam("UserID", credentials.getUsername())
                .queryParam("Password", credentials.getPassword())
                .queryParam("MobileNo", credentials.getUsername())
                .queryParam("Message", message)
                .toUriString();

        // 🔹 Call Vendor API
        String vendorResponse =
                restTemplate.getForObject(url, String.class);

        return vendorResponse;
    }

    @Override
    public String checkStatus(RechargeRequest request, VendorCredentials credentials) {

        Retailer retailer = retailerRepository.findById(request.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));


        // Build Message
        String message = "STATUS$"
                + request.getTransactionRef() + "$"
                + SMS_PIN;

        // Build URL
        String url = UriComponentsBuilder
                .fromHttpUrl(credentials.getBaseUrlUat())
                .queryParam("UserID", credentials.getUsername())
                .queryParam("Password", credentials.getPassword())
                .queryParam("MobileNo", credentials.getUsername())
                .queryParam("Message", message)
                .toUriString();

        //Call Vendor API
        String vendorResponse =
                restTemplate.getForObject(url, String.class);

        return vendorResponse;
    }

    @Override
    public String checkStatusByTxnRef(String transactionRef,
                                      VendorCredentials credentials) {

        String message = "STATUS$"
                + transactionRef + "$"
                + SMS_PIN;

        String url = UriComponentsBuilder
                .fromHttpUrl(credentials.getBaseUrlUat())
                .queryParam("UserID", credentials.getUsername())
                .queryParam("Password", credentials.getPassword())
                .queryParam("MobileNo", credentials.getUsername())
                .queryParam("Message", message)
                .toUriString();

        return restTemplate.getForObject(url, String.class);
    }
}

