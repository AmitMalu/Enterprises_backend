package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Entity
@Table(name = "white_labels")
@Getter
@Setter
public class WhiteLabels {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "IM_account_member_ID")
    private String imAccountMemberID;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "address")
    private String address;

    @Column(name = "domain_url")
    private String domainURL;

    @Column(name = "terms_and_condition_url")
    private String termsAndConditionURL;

    @Column(name = "privacy_policy_url")
    private String privacyPolicyURL;

    @Column(name = "refund_and_cancellation_url")
    private String refundAndCancellationURL;

    @Column(name = "is_service_allow", nullable = false)
    private Boolean isServiceAllow = false;

    @Column(name = "allow_prepaid_cards")
    private Boolean allowPrepaidCards;

    @Column(name = "prepaid_card_name")
    private String prepaidCardName;

    @Column(name = "allow_postpaid_cards")
    private Boolean allowPostpaidCards;

    @Column(name = "postpaid_card_name")
    private String postpaidCardName;

//    @ManyToMany
//    @JoinTable(name = "white_label_products",
//            joinColumns = @JoinColumn(name = "white_label_id"),  // Foreign key to WhiteLabels
//            inverseJoinColumns = @JoinColumn(name = "product_id")) // Foreign key to Products
//    private Set<Products> products;

    @Column(name = "products", columnDefinition = "TEXT")
    private String products; // Store as a comma-separated string

    @Column(name = "is_redirect")
    private Boolean isRedirect;

    @Column(name = "appID")
    private String appID;

    @Column(name = "captcha_site_key")
    private String captchaSiteKey;

    @Column(name = "captcha_secret_key")
    private String captchaSecretKey;

    @Column(name = "remarks")
    private String remarks;

    @ManyToOne
    @JoinColumn(name = "theme_id", nullable = true)
    private Theme theme;

    // Getter & Setter for products as List
    public List<String> getProductsList() {
        return products != null ? Arrays.asList(products.split(",")) : new ArrayList<>();
    }

    public void setProductsList(List<String> productsList) {
        this.products = String.join(",", productsList);
    }

}
