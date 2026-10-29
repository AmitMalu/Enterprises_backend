package com.scs.admin.dto;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class MenuRequestDto {
    private String menuName;
    private String subMenuName;
}
