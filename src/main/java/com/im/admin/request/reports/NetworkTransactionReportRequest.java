package com.im.admin.request.reports;


import com.im.admin.entities.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetworkTransactionReportRequest {

    private String fromDate;   // Format: yyyy-MM-dd
    private String toDate;     // Format: yyyy-MM-dd
    private String subProductName;
    private String enterprisesId;
    private String whitelabelId;
    private String superDistributorId;
    private String partnerId;
    private String distributorId;
    private String retailerId;
    private String loginHierarchyId;
    private Status status;

    private int page = 0;
    private int size = 10;

    //i have this request using that we have to fetch the data  like we do in myTransaction api like that but littel chnage in this
    //report in this there is no wallet type so both prepaid and postpaid we have to fetch records anf there is one more check we have to
    //is whoever hierarchy login its wheather white label or super distributor or anyone so just keep in mind login hierarcy id is mandatory and below
    //his at least one hierarchy is selected
    //means if loginhierarhcy is super distributor under his partner , distributor and retailer come so at least partner need to be select to view the transaction
    //then he will deide based on partner select under his dsitributr list will come then he willl select paeticular distributor other wise
    //he will not he just select partner and view like that this applicable everry time wheater its white label , enterprises or admin
    //admin can view all transactions of his hierarhcy based on select hierarhcy
}
