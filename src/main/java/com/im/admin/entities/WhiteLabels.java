package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Entity
@Table(name = "white_labels")
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDomainURL() {
        return domainURL;
    }

    public void setDomainURL(String domainURL) {
        this.domainURL = domainURL;
    }

    public String getTermsAndConditionURL() {
        return termsAndConditionURL;
    }

    public void setTermsAndConditionURL(String termsAndConditionURL) {
        this.termsAndConditionURL = termsAndConditionURL;
    }

    public String getPrivacyPolicyURL() {
        return privacyPolicyURL;
    }

    public void setPrivacyPolicyURL(String privacyPolicyURL) {
        this.privacyPolicyURL = privacyPolicyURL;
    }

    public String getRefundAndCancellationURL() {
        return refundAndCancellationURL;
    }

    public void setRefundAndCancellationURL(String refundAndCancellationURL) {
        this.refundAndCancellationURL = refundAndCancellationURL;
    }

    public Boolean getServiceAllow() {
        return isServiceAllow;
    }

    public void setServiceAllow(Boolean serviceAllow) {
        isServiceAllow = serviceAllow;
    }

    public Boolean getAllowPrepaidCards() {
        return allowPrepaidCards;
    }

    public void setAllowPrepaidCards(Boolean allowPrepaidCards) {
        this.allowPrepaidCards = allowPrepaidCards;
    }

    public String getPrepaidCardName() {
        return prepaidCardName;
    }

    public void setPrepaidCardName(String prepaidCardName) {
        this.prepaidCardName = prepaidCardName;
    }

    public Boolean getAllowPostpaidCards() {
        return allowPostpaidCards;
    }

    public void setAllowPostpaidCards(Boolean allowPostpaidCards) {
        this.allowPostpaidCards = allowPostpaidCards;
    }

    public String getPostpaidCardName() {
        return postpaidCardName;
    }

    public void setPostpaidCardName(String postpaidCardName) {
        this.postpaidCardName = postpaidCardName;
    }

    public String getProducts() {
        return products;
    }

    public void setProducts(String products) {
        this.products = products;
    }

    public Boolean getRedirect() {
        return isRedirect;
    }

    public void setRedirect(Boolean redirect) {
        isRedirect = redirect;
    }

    public String getAppID() {
        return appID;
    }

    public void setAppID(String appID) {
        this.appID = appID;
    }

    public String getCaptchaSiteKey() {
        return captchaSiteKey;
    }

    public void setCaptchaSiteKey(String captchaSiteKey) {
        this.captchaSiteKey = captchaSiteKey;
    }

    public String getImAccountMemberID() {
        return imAccountMemberID;
    }

    public void setImAccountMemberID(String imAccountMemberID) {
        this.imAccountMemberID = imAccountMemberID;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getCaptchaSecretKey() {
        return captchaSecretKey;
    }

    public void setCaptchaSecretKey(String captchaSecretKey) {
        this.captchaSecretKey = captchaSecretKey;
    }


}
