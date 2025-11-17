package com.im.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SendOtpRequest {

    private String mobile;
    private String txntype;     // IMPS | NEFT
    private String amount;
    private String pincode;
    private String address;
    private String dob;         // DD-MM-YYYY
    @JsonProperty("gst_state")
    private String gstState;
    private String lat;
    private String lng;
    private String referenceid;
    private String beneId;
}
