package com.im.admin.request;

import com.im.admin.dto.UpdateTransactionStatusRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BulkUpdateTransactionStatusRequest {

    private List<UpdateTransactionStatusRequest> transactions;

}

