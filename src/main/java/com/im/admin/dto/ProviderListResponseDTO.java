package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProviderListResponseDTO {

    private int status;
    private String message;
    private List<ProviderDTO> providerList;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProviderDTO {

        private int provider_id;
        private String provider_name;
        private String service_name;
        private List<ExtraParamDTO> extra_param;
        private boolean is_amount_editable;
        private boolean pay_without_fetch;
        private boolean is_numeric;
        private int min_length;
        private int max_length;
        private int min_amount;
        private int max_amount;
        private String bill_due_date;
        private String consumerNumber;
        private boolean is_bill_due_date_required;

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExtraParamDTO {
        private String field_one;
        private String field_two;
        private String field_three;
        private String field_four;
        private List<ExtraParamContentDTO> extraparam_content;

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExtraParamContentDTO {
        private String name;
    }

}
