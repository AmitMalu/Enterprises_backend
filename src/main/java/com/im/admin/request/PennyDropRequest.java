package com.im.admin.request;


import lombok.Data;

@Data
public class PennyDropRequest {

    private String mobile;
    private String accno;
    private String bankid;
    private String benename;
    private String referenceid;
    private String pincode;
    private String address;
    private String dob;
    private String gst_state;
    private String bene_id;
    private String SubProductId;
}
