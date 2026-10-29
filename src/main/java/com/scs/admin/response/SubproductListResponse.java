package com.scs.admin.response;

/**
 * @author SHUBHAM KHOPADE
 */
import com.scs.admin.dto.SubproductDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubproductListResponse {

    private String status;

    private String msg;

    @Builder.Default
    private List<SubproductDto> data = new ArrayList<>();
}
