package com.im.admin.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DropdownResponseDto {
    private List<DropdownOptionDto> alertType;
    private List<DropdownOptionDto> audienceType;
    private List<DropdownOptionDto> product;
}
