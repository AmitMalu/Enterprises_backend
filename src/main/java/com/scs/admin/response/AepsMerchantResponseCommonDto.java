package com.scs.admin.response;

import lombok.Data;

@Data
public class AepsMerchantResponseCommonDto {

    private String statuscode;
    private String actcode;
    private String status;
    private DataDetails data;

    private String timestamp;
    private String ipay_uuid;
    private String orderid;
    private String environment;
    private String internalCode;

    @Data
    public static class DataDetails {

        private Integer outletId;
        private String name;
        private String dateOfBirth;
        private String gender;
        private String pincode;
        private String state;
        private String districtName;
        private String address;
        private String profilePic;

    }
}