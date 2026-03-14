package com.im.admin.mapper;

import com.im.admin.response.AepsMerchantResponseCommonDto;
import com.im.admin.response.AepsMerchantResponseDto;
import com.im.admin.response.InstantpayMerchantResponseDto;

public class AepsVendorMapper {

//    public static AepsMerchantResponseDto fromFingpay(
//            FingpayMerchantResponseDto res) {
//
//        AepsMerchantResponseDto dto = new AepsMerchantResponseDto();
//
//        dto.setSuccess(res.isStatus());
//        dto.setMessage(res.getMessage());
//
//        if (res.getData() != null) {
//            dto.setMerchantId(res.getData().getMerchantLoginId());
//        }
//
//        dto.setVendor("FINGPAY");
//        dto.setStatus("ACTIVE");
//
//        return dto;
//    }


    public static AepsMerchantResponseDto fromInstantpay(
            InstantpayMerchantResponseDto res) {

        AepsMerchantResponseDto dto = new AepsMerchantResponseDto();

        boolean success = "TXN".equalsIgnoreCase(res.getStatuscode());

        dto.setStatus(success);
        dto.setMessage(res.getStatus());
        dto.setStatusCode(success ? 200 : 400);

        AepsMerchantResponseDto.DataDetails data =
                new AepsMerchantResponseDto.DataDetails();

        if (res.getData() != null) {

            data.setMerchantStatus(success);
            data.setRemarks(res.getStatus());
            data.setMerchantLoginId(
                    String.valueOf(res.getData().getOutletId())
            );
        }

        dto.setData(data);

        return dto;
    }
}