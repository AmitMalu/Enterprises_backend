package com.im.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "partner")
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "parent_account", nullable = false)
    private String parentAccount;

    @Embedded
    @Valid
    private Info info;

    @Column(name = "competitor")
    private String competitor;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "main_address")),
            @AttributeOverride(name = "city", column = @Column(name = "main_city")),
            @AttributeOverride(name = "state", column = @Column(name = "main_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "main_zip_code"))
    })
    @Valid
    private Address address;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "outlet_address")),
            @AttributeOverride(name = "city", column = @Column(name = "outlet_city")),
            @AttributeOverride(name = "state", column = @Column(name = "outlet_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "outlet_zip_code"))
    })
    @Valid
    private Address outletAddress;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Address getOutletAddress() {
        return outletAddress;
    }

    public void setOutletAddress(Address outletAddress) {
        this.outletAddress = outletAddress;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getCompetitor() {
        return competitor;
    }

    public void setCompetitor(String competitor) {
        this.competitor = competitor;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public String getParentAccount() {
        return parentAccount;
    }

    public void setParentAccount(String parentAccount) {
        this.parentAccount = parentAccount;
    }
}