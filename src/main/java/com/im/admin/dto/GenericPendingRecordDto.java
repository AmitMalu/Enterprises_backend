package com.im.admin.dto;

import com.im.admin.entities.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GenericPendingRecordDto {

    private String type;         // e.g., RETAILER, ADMIN, etc.
    private String id;           // unique ID per type (retailerId, adminId, etc.)
    private String name;         // name of the person or business
    private String status;
    private String createdBy;
    private String createdOn;
    private String mobileNumber;
    private String emailId;
    private String parentAccount;
    private String remarks;
    private String editedBy;
    private String lastEditedOn;
    private String approvedBy;
    private String approvedOn;
    private String rejectedBy;
    private String rejectedOn;

}
