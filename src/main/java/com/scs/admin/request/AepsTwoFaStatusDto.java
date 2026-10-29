package com.scs.admin.request;

import com.scs.admin.entities.AepsTwoFa;
import lombok.Data;

@Data
public class AepsTwoFaStatusDto {

    private int twoFaStatus;
    private AepsTwoFa twoFaData;
}
