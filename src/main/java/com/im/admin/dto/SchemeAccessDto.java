package com.im.admin.dto;

/**
 * @author SHUBHAM KHOPADE
 */
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SchemeAccessDto {

    private boolean fullAccess;

    private List<String> allowedSubProducts;

}
