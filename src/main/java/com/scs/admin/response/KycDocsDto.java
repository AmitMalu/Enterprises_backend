package com.scs.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KycDocsDto {

    private String aadhaarCardFrontImage;
    private String aadhaarCardBackImage;
    private String panCardImage;
    private String shopImage;
    private String businessProofImage;

}
