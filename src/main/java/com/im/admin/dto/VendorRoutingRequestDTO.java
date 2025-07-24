package com.im.admin.dto;

import com.im.admin.entities.vendor.Vendors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VendorRoutingRequestDTO {

    private String subProductId;

    private VendorsDTO routingPriority1;
    private VendorsDTO routingPriority2;
    private VendorsDTO routingPriority3;

    private String volume1;
    private String volume2;
    private String volume3;

    private Integer volumeNumberOfTxn;
    private Integer volumeValueOfTxn;

    private String value1;
    private String value2;
    private String value3;

    private Integer valueValueNumberOfTxn;
    private Integer valueValueOfTxn;

    private String createdBy;
    private String editedBy;

}

