package com.scs.admin.dto;

import com.scs.admin.entities.product.Products;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OperatorProductResponseDTO {

    private List<OperatorDropdownResponseDTO> operators;
    private Products product;

}
