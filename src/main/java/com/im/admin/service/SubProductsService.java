package com.im.admin.service;

import com.im.admin.dto.SubProductDropdownResponseDto;
import com.im.admin.dto.SubProductsResponseDTO;
import com.im.admin.entities.SubProducts;
import com.im.admin.request.SubproductListRequest;
import com.im.admin.response.SubproductListResponse;
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

    SubproductListResponse getSubProductList(
            SubproductListRequest request, String geoLocation
    );

}

