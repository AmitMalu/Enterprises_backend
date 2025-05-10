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
public class PendingStatusResponseDto {

    private List<Retailer> retailers;
    private List<Distributor> distributors;
    private List<Partner> partners;
    private List<SuperDistributor> superDistributors;
    private List<WhiteLabels> whiteLabels;
    private List<Enterprises> enterprises;
    private List<Admin> admins;

}
