package com.scs.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class RemapRequestDTO {
    private List<String> userIds;
    private String newParentId;
}
