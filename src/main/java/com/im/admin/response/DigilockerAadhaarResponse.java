package com.im.admin.response;

/**
 * @author SHUBHAM KHOPADE
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DigilockerAadhaarResponse {

    private DataBody data;

    @JsonProperty("status_code")
    private int statusCode;

    private boolean success;
    private String message;

    @JsonProperty("message_code")
    private String messageCode;

    @Data
    public static class DataBody {

        @JsonProperty("client_id")
        private String clientId;

        @JsonProperty("digilocker_metadata")
        private DigilockerMetadata digilockerMetadata;

        @JsonProperty("aadhaar_xml_data")
        private AadhaarXmlData aadhaarXmlData;

        @JsonProperty("xml_url")
        private String xmlUrl;
    }

    @Data
    public static class DigilockerMetadata {

        private String name;
        private String gender;
        private String dob;

        @JsonProperty("mobile_number")
        private String mobileNumber;
    }

    @Data
    public static class AadhaarXmlData {

        @JsonProperty("full_name")
        private String fullName;

        @JsonProperty("care_of")
        private String careOf;

        private String dob;
        private String yob;
        private String zip;

        @JsonProperty("profile_image")
        private String profileImage;

        private String gender;

        @JsonProperty("masked_aadhaar")
        private String maskedAadhaar;

        @JsonProperty("full_address")
        private String fullAddress;

        @JsonProperty("father_name")
        private String fatherName;

        private Address address;
    }

    @Data
    public static class Address {

        private String country;
        private String dist;
        private String state;
        private String po;
        private String house;
        private String loc;
        private String vtc;
        private String subdist;
        private String street;
        private String landmark;
    }
}