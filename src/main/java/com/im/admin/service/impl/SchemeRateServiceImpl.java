package com.im.admin.service.impl;


import com.im.admin.dto.*;
import com.im.admin.entities.Scheme;
import com.im.admin.entities.SchemeHierarchySelection;
import com.im.admin.entities.SchemeRate;
import com.im.admin.entities.enums.RateType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.repositories.SchemeHierarchySelectionRepository;
import com.im.admin.repositories.SchemeRateRepository;
import com.im.admin.repositories.SchemeRepository;
import com.im.admin.request.OperatorWithSlabsDto;
import com.im.admin.request.SlabRateDto;
import com.im.admin.response.SchemeDetailResponseDto;
import com.im.admin.service.SchemeRateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SchemeRateServiceImpl implements SchemeRateService {

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private SchemeHierarchySelectionRepository schemeHierarchySelectionRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Override
    public SchemeDetailResponseDto getSchemeDetails(String schemeCode) {
        Scheme scheme = schemeRepository.findBySchemeCode(schemeCode)
                .orElseThrow(() -> new RuntimeException("Scheme not found"));

        // Extract schemeHierarchyId from schemeCombinationHashCode (e.g., "10-01-02-001-00001")
        String[] parts = scheme.getSchemeCombinationHashCode().split("-");
        if (parts.length < 4) {
            throw new RuntimeException("Invalid combination hash code format");
        }
        String schemeHierarchyId = parts[3]; // "001"

        // Fetch hierarchy combination record
        SchemeHierarchySelection hierarchySelection = schemeHierarchySelectionRepository.findById(schemeHierarchyId)
                .orElseThrow(() -> new RuntimeException("Scheme hierarchy not found"));

        String hierarchyCombination = hierarchySelection.getSchemeHierarchyCombination(); // e.g., "Distributor-Retailer-Partner"

        // Convert it to a Set for quick checking
        Set<String> allowedHierarchies = Arrays.stream(hierarchyCombination.split("-"))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Fetch all rates by scheme code
        List<SchemeRate> rates = schemeRateRepository.findBySchemeCode(schemeCode);

        // Group by operator code
        Map<String, List<SchemeRate>> groupedByOperator = rates.stream()
                .collect(Collectors.groupingBy(SchemeRate::getOperatorCode));

        List<OperatorWithSlabsDto> operatorDtos = groupedByOperator.entrySet().stream().map(entry -> {
            String operatorCode = entry.getKey();
            List<SchemeRate> operatorSlabs = entry.getValue();

            OperatorWithSlabsDto operatorDto = new OperatorWithSlabsDto();
            operatorDto.setOperatorCode(operatorCode);
            operatorDto.setOperatorName(operatorSlabs.get(0).getOperatorName());

            List<SlabRateDto> slabs = operatorSlabs.stream().map(slab -> {
                SlabRateDto dto = new SlabRateDto();

                // Common slab fields
                dto.setId(slab.getId());
                dto.setRateType(slab.getRateType());
                dto.setSlabType(slab.getSlabType());
                dto.setFixedSlabValue(slab.getFixedSlabValue());
                dto.setSlabStart(slab.getSlabStart());
                dto.setSlabEnd(slab.getSlabEnd());

                // Conversion logic based on RateType
                Double factor = slab.getRateType() == RateType.PERCENTAGE ? 100.0 : 1.0;

                dto.setGross(safeMultiply(slab.getGross(), factor));
                dto.setIm(safeMultiply(slab.getIm(), factor));
                dto.setEnterprise(safeMultiply(slab.getEnterprise(), factor));

                if (allowedHierarchies.contains("whitelabel")) {
                    dto.setWhitelabel(safeMultiply(slab.getWhitelabel(), factor));
                }
                if (allowedHierarchies.contains("superdistributor") || allowedHierarchies.contains("super_distributor")) {
                    dto.setSuperDistributor(safeMultiply(slab.getSuperDistributor(), factor));
                }
                if (allowedHierarchies.contains("partner")) {
                    dto.setPartner(safeMultiply(slab.getPartner(), factor));
                }
                if (allowedHierarchies.contains("distributor")) {
                    dto.setDistributor(safeMultiply(slab.getDistributor(), factor));
                }
                if (allowedHierarchies.contains("retailer")) {
                    dto.setRetailer(safeMultiply(slab.getRetailer(), factor));
                }

                return dto;
            }).collect(Collectors.toList());


            operatorDto.setSlabs(slabs);
            return operatorDto;
        }).collect(Collectors.toList());

        SchemeDetailResponseDto responseDto = new SchemeDetailResponseDto();
        responseDto.setSchemeCode(scheme.getSchemeCode());
        responseDto.setSchemeName(scheme.getSchemeName());
        responseDto.setOperators(operatorDtos);

        return responseDto;
    }

    private Double safeMultiply(Double value, Double factor) {
        return value == null ? null : value * factor;
    }


    @Override
    public List<SchemeRate> createSchemeRates(SchemeRateRequestDTO requestDTO) {
        List<SchemeRate> schemeRates = new ArrayList<>();

        for (int i = 0; i < requestDTO.getSlabs().size(); i++) {
            SchemeRateSlabDTO slab = requestDTO.getSlabs().get(i);
            SchemeRate schemeRate = new SchemeRate();

            schemeRate.setSchemeCode(requestDTO.getSchemeCode());
            schemeRate.setOperatorName(requestDTO.getOperatorName());
            schemeRate.setOperatorCode(requestDTO.getOperatorCode());
            schemeRate.setRateType(slab.getRateType());
            schemeRate.setSlabStart(slab.getSlabStart());
            schemeRate.setSlabEnd(slab.getSlabEnd());

            // Ensure gross concept: sum of all fields must be exactly equal to gross
            double im = slab.getIm() == null ? 0.0 : slab.getIm();
            double enterprise = slab.getEnterprise() == null ? 0.0 : slab.getEnterprise();
            double whitelabel = slab.getWhitelabel() == null ? 0.0 : slab.getWhitelabel();
            double superDistributor = slab.getSuperDistributor() == null ? 0.0 : slab.getSuperDistributor();
            double partner = slab.getPartner() == null ? 0.0 : slab.getPartner();
            double distributor = slab.getDistributor() == null ? 0.0 : slab.getDistributor();
            double retailer = slab.getRetailer() == null ? 0.0 : slab.getRetailer();

            double gross = slab.getGross();
            validateGrossMatch(
                    gross, im, enterprise, whitelabel, superDistributor,
                    partner, distributor, retailer, "slab #" + (i + 1)
            );


            // Store values based on rateType
            if (slab.getRateType() == RateType.PERCENTAGE) {
                schemeRate.setGross(gross / 100);
                schemeRate.setIm(im / 100);
                schemeRate.setEnterprise(enterprise / 100);
                schemeRate.setWhitelabel(whitelabel / 100);
                schemeRate.setSuperDistributor(superDistributor / 100);
                schemeRate.setPartner(partner / 100);
                schemeRate.setDistributor(distributor / 100);
                schemeRate.setRetailer(retailer / 100);
            } else {
                schemeRate.setGross(gross);
                schemeRate.setIm(im);
                schemeRate.setEnterprise(enterprise);
                schemeRate.setWhitelabel(whitelabel);
                schemeRate.setSuperDistributor(superDistributor);
                schemeRate.setPartner(partner);
                schemeRate.setDistributor(distributor);
                schemeRate.setRetailer(retailer);
            }

            schemeRate.setCreatedBy(slab.getCreatedBy());
            schemeRates.add(schemeRate);
        }

        return schemeRateRepository.saveAll(schemeRates);
    }

    @Override
    @Transactional
    public SchemeDetailResponseDto updateSchemeRates(SchemeSlabUpdateRequestDTO requestDto) {
        for (OperatorWithSlabsDto operator : requestDto.getOperators()) {
            for (SlabRateDto slabDto : operator.getSlabs()) {
                SchemeRate slab;

                if (slabDto.getId() != null) {
                    slab = schemeRateRepository.findById(slabDto.getId())
                            .orElseThrow(() -> new RuntimeException("Slab not found for ID: " + slabDto.getId()));
                    slab.setCreatedBy(slabDto.getCreatedBy());
                } else {
                    slab = new SchemeRate();
                    slab.setSchemeCode(requestDto.getSchemeCode());
                    slab.setOperatorCode(operator.getOperatorCode());
                    slab.setOperatorName(operator.getOperatorName());
                    slab.setCreatedBy(slabDto.getCreatedBy());
                }

                slab.setRateType(slabDto.getRateType());
                slab.setSlabType(slabDto.getSlabType());
                slab.setFixedSlabValue(slabDto.getFixedSlabValue());
                slab.setSlabStart(slabDto.getSlabStart());
                slab.setSlabEnd(slabDto.getSlabEnd());

                double gross = safeValue(slabDto.getGross());
                double im = safeValue(slabDto.getIm());
                double enterprise = safeValue(slabDto.getEnterprise());
                double whitelabel = safeValue(slabDto.getWhitelabel());
                double superDistributor = safeValue(slabDto.getSuperDistributor());
                double partner = safeValue(slabDto.getPartner());
                double distributor = safeValue(slabDto.getDistributor());
                double retailer = safeValue(slabDto.getRetailer());

                validateGrossMatch(
                        gross, im, enterprise, whitelabel, superDistributor,
                        partner, distributor, retailer,
                        "slab ID " + (slabDto.getId() != null ? slabDto.getId() : "NEW")
                );

                double factor = (slabDto.getRateType() == RateType.PERCENTAGE) ? 0.01 : 1.0;

                slab.setGross(gross * factor);
                slab.setIm(im * factor);
                slab.setEnterprise(enterprise * factor);
                slab.setWhitelabel(whitelabel * factor);
                slab.setSuperDistributor(superDistributor * factor);
                slab.setPartner(partner * factor);
                slab.setDistributor(distributor * factor);
                slab.setRetailer(retailer * factor);
                slab.setEditedBy(slabDto.getEditedBy());

                schemeRateRepository.save(slab);
            }
        }

        return getSchemeDetails(requestDto.getSchemeCode());
    }

    private double safeValue(Double val) {
        return val == null ? 0.0 : val;
    }

    @Override
    @Transactional(readOnly = true)
    public SchemeRateProductResponseDto getProductDetails(String schemeCode) {

        Scheme scheme = schemeRepository.findBySchemeCode(schemeCode)
                .orElseThrow(() -> new RuntimeException("Scheme not found for scheme code: " + schemeCode));

        String[] parts = scheme.getSchemeCombinationHashCode().split("-");
        if (parts.length < 2) {
            throw new RuntimeException("Invalid scheme combination hash code format for scheme code: " + schemeCode);
        }

        short productId;
        try {
            productId = Short.parseShort(parts[0]);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid product ID in hash code for scheme code: " + schemeCode, e);
        }

        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found for ID: " + productId));

        return mapToProductResponseDto(product);
    }

    private SchemeRateProductResponseDto mapToProductResponseDto(Products product) {
        SchemeRateProductResponseDto dto = new SchemeRateProductResponseDto();
        dto.setProductName(product.getName());
        dto.setProductCode(product.getProductCode());
        dto.setIsSettlement(product.getIsSettlement());
        dto.setProductCodeForScheme(product.getProductCodeForScheme());
        dto.setTransactionType(product.getTransactionType());
        dto.setTds(product.getTds());
        dto.setSgst(product.getSgst());
        dto.setEditedBy(product.getEditedBy());
        dto.setEditedAt(product.getEditedOn());
        dto.setCreatedBy(product.getCreatedBy());
        dto.setCreatedAt(product.getCreatedOn());
        return dto;
    }

    private void validateGrossMatch(
            double gross,
            Double im,
            Double enterprise,
            Double whitelabel,
            Double superDistributor,
            Double partner,
            Double distributor,
            Double retailer,
            String errorReference
    ) {
        double imVal = im == null ? 0.0 : im;
        double enterpriseVal = enterprise == null ? 0.0 : enterprise;
        double whitelabelVal = whitelabel == null ? 0.0 : whitelabel;
        double superDistVal = superDistributor == null ? 0.0 : superDistributor;
        double partnerVal = partner == null ? 0.0 : partner;
        double distributorVal = distributor == null ? 0.0 : distributor;
        double retailerVal = retailer == null ? 0.0 : retailer;

        double total = imVal + enterpriseVal + whitelabelVal + superDistVal + partnerVal + distributorVal + retailerVal;

        if (!Objects.equals(gross, total)) {
            throw new IllegalArgumentException(
                    "Error in " + errorReference + ": Sum of all shares must be equal to Gross. Expected: "
                            + gross + ", Found: " + total
            );
        }
    }

}
