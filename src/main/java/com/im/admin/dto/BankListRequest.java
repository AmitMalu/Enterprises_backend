package com.im.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
public class BankListRequest {

    @JsonProperty("BANKLIST")
    private List<BankDto> bankList;
}
