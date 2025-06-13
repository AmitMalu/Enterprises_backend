package com.im.admin.dto;

import com.im.admin.entities.product.Products;

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
