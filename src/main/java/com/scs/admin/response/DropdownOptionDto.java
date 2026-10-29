package com.scs.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DropdownOptionDto {

    private String label;
    private String value;
}