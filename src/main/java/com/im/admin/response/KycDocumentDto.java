package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class KycDocumentDto {
    private String docName;
    private String docStatus;
    private String docNumber;
    private String docImage;
}
