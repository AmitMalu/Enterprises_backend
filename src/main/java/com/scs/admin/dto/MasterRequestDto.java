package com.scs.admin.dto;


import lombok.Data;

@Data
public class MasterRequestDto {
    private String scsMasterName;       // e.g., Currency
    private String scsSubMasterName;     // e.g., Rs, AED, USD
    private String createdBy;
}
