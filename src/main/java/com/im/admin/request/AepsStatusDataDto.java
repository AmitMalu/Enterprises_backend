package com.im.admin.request;

import lombok.Data;

@Data
public class AepsStatusDataDto {
    private int onboardStatus;

    private Profile profile;       // new added field

    @Data
    public static class Profile {
        private String retailerId;
        private String name;
        private String mobileNumber;
        private String emailId;
        private String dob;   // String because LocalDate → convert
        private String aadhaarNumber;
        private String panNumber;
    }

}
