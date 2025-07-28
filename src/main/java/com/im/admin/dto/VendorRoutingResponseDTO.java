package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VendorRoutingResponseDTO {

    private Long id;
    private String subProductId;
    private String subProductName;

    private String routingPriority1;
    private String routingPriority1Name;

    private String routingPriority2;
    private String routingPriority2Name;

    private String routingPriority3;
    private String routingPriority3Name;

    private String volume1;
    private String volume2;
    private String volume3;

    private Integer volumeNumberOfTxn;
    private Integer VolumeValueOfTxn;

    private String value1;
    private String value2;
    private String value3;

    private Integer ValueValueNumberOfTxn;
    private Integer ValueValueOfTxn;

    private String createdBy;
    private String editedBy;
}

