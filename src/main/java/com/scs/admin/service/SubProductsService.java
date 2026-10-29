package com.scs.admin.service;

import com.scs.admin.dto.SubProductDropdownResponseDto;
import com.scs.admin.dto.SubProductsResponseDTO;
import com.scs.admin.dto.SubproductDto;
import com.scs.admin.entities.SubProducts;
import com.scs.admin.request.SubproductListRequest;
import com.scs.admin.response.SubproductListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface SubProductsService {

    SubProducts saveSubProduct(SubProducts subProduct);

    void deleteSubProduct(String id);

    SubProducts updateSubProduct(String id, SubProducts subProduct);

    Page<SubProductsResponseDTO> getAllSubProducts(Pageable pageable, List<String> id,
                                                   String subProductTitle, String subProductCode, String productCode,
                                                   Short productId, String productName, String walletType,
                                                   Boolean isSettlement, Boolean isActive, Boolean enabled,
                                                   String createdBy, String editedBy,
                                                   LocalDate createdOn, LocalDate editedOn);

    SubProductsResponseDTO getSubProductById(String id);

    List<SubProductDropdownResponseDto> getActiveSubProductsByProductId(Short productId,Boolean isSettlement);

    List<SubProductDropdownResponseDto> getSubProductDropdown();

    List<SubproductDto> getSubProductList(
            SubproductListRequest request,
            String geoLocation
    );

}

