package com.im.admin.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RunPaisaCallbackWrapper {

    @JsonProperty("CALLBACKURL")
    private List<RunPaisaCallback> callbackList;

}

