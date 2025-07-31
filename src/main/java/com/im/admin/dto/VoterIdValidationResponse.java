package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoterIdValidationResponse {
    private VoterData data;
    private int status_code;
    private boolean success;
    private String message;
    private String message_code;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VoterData {
        private String client_id;
        private String input_voter_id;
        private String epic_no;
        private String gender;
        private String state;
        private String name;
        private String relation_name;
        private String relation_type;
        private String house_no;
        private String dob;
        private String age;
        private String area;
        private String district;
        private List<String> additional_check;
        private boolean multiple;
        private String last_update;
        private String assembly_constituency;
        private String assembly_constituency_number;
        private String polling_station;
        private String part_number;
        private String part_name;
        private String slno_inpart;
        private String ps_lat_long;
        private String rln_name_v1;
        private String rln_name_v2;
        private String rln_name_v3;
        private String section_no;
        private String name_v1;
        private String name_v2;
        private String name_v3;
        private String parliamentary_name;
        private String parliamentary_number;
        private String st_code;
        private String parliamentary_constituency;
        private String id;
    }
}
