package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HierarchyUpdateDTO {
    private String id;             // RetailerId, DistributorId, etc.
    private String parentAccount;  // Updated parent
    private String name;           // Entity name
    private String mobileNumber;
}
