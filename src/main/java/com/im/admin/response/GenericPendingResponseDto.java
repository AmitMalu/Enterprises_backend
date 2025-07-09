package com.im.admin.response;

import com.im.admin.dto.GenericPendingRecordDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenericPendingResponseDto {
    private List<GenericPendingRecordDto> records;
}

