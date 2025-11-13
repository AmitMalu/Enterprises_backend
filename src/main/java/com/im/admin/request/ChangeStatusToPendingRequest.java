package com.im.admin.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeStatusToPendingRequest {

    private String userId;
    private String remarks;
}
