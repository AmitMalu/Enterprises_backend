package com.scs.admin.mapper;

import com.scs.admin.entities.Scheme;
import com.scs.admin.entities.SchemeHierarchySelection;
import com.scs.admin.entities.SubProducts;
import com.scs.admin.entities.product.Products;
import com.scs.admin.entities.vendor.Vendors;
import com.scs.admin.repositories.ProductsRepository;
import com.scs.admin.repositories.SchemeHierarchySelectionRepository;
import com.scs.admin.repositories.SubProductsRepository;
import com.scs.admin.repositories.VendorRepository;
import com.scs.admin.response.GetAllSchemeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author SHUBHAM KHOPADE
 */
@Component
@RequiredArgsConstructor
public class SchemeMapper {

    private final ProductsRepository productsRepository;

    private final SubProductsRepository subProductsRepository;

    private final VendorRepository vendorRepository;

    private final SchemeHierarchySelectionRepository
            hierarchyRepo;

    public GetAllSchemeResponse toResponse(
            Scheme scheme
    ) {

        String[] parts =
                scheme
                        .getSchemeCombinationHashCode()
                        .split("-");

        String productId =
                getPart(parts, 0);

        String subProductId =
                getPart(parts, 1);

        String vendorId =
                getPart(parts, 2);

        String hierarchyId =
                getPart(parts, 3);

        return new GetAllSchemeResponse(

                scheme.getId(),

                scheme.getSchemeName(),

                scheme.getSchemeCode(),

                scheme.getSchemeCombinationHashCode(),

                productId,

                getProductName(
                        productId
                ),

                subProductId,

                getSubProductName(
                        subProductId
                ),

                vendorId,

                getVendorName(
                        vendorId
                ),

                getHierarchyName(
                        hierarchyId
                ),

                hierarchyId,

                scheme.getCreatedOn(),

                scheme.getCreatedBy(),

                scheme.getEditedOn(),

                scheme.getEditedBy(),

                scheme.getIsActive()
        );
    }

    private String getPart(
            String[] parts,
            int index
    ) {

        return parts.length > index
                ? parts[index]
                : null;
    }

    private String getProductName(
            String id
    ) {

        if (id == null)
            return "N/A";

        return productsRepository
                .findById(
                        Short.valueOf(id)
                )
                .map(
                        Products::getName
                )
                .orElse("N/A");
    }

    private String getSubProductName(
            String id
    ) {

        if (id == null)
            return "N/A";

        return subProductsRepository
                .findById(id)
                .map(
                        SubProducts::
                                getSubProductTitle
                )
                .orElse("N/A");
    }

    private String getVendorName(
            String id
    ) {

        if (id == null)
            return "N/A";

        return vendorRepository
                .findById(id)
                .map(
                        Vendors::
                                getVendorName
                )
                .orElse("N/A");
    }

    private String getHierarchyName(
            String id
    ) {

        if (id == null)
            return "N/A";

        return hierarchyRepo
                .findByCustomId(id)
                .map(
                        SchemeHierarchySelection::
                                getSchemeHierarchyCombination
                )
                .orElse("N/A");
    }
}
