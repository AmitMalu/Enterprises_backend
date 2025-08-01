package com.im.admin.service.impl;


import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.RateType;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.request.OperatorWithSlabsDto;
import com.im.admin.request.SlabRateDto;
import com.im.admin.response.SchemeDetailResponseDto;
import com.im.admin.service.SchemeRateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private OperatorsRepository operatorRepository;

//    @Override
//    public SchemeDetailResponseDto getSchemeDetails(String schemeCode) {
//        log.info("Fetching scheme details for schemeCode: {}", schemeCode);
//
//        Scheme scheme = schemeRepository.findBySchemeCode(schemeCode)
//                .orElseThrow(() -> {
//                    log.error("Scheme not found for schemeCode: {}", schemeCode);
//                    return new RuntimeException("Scheme not found");
//                });
//
//        log.debug("Scheme found: {}", scheme.getSchemeName());
//
//        String[] parts = scheme.getSchemeCombinationHashCode().split("-");
//        if (parts.length < 4) {
//            log.error("Invalid scheme combination hash: {}", scheme.getSchemeCombinationHashCode());
//            throw new RuntimeException("Invalid combination hash code format");
//        }
//
//        String productId = parts[0];
//        String subProductId = parts[1];
//        String schemeHierarchyId = parts[3];
//
//        log.debug("Parsed combination hash - productId: {}, subProductId: {}, schemeHierarchyId: {}", productId, subProductId, schemeHierarchyId);
//
//        Products product = productsRepository.findById(Short.valueOf(productId))
//                .orElseThrow(() -> {
//                    log.error("Product not found for productId: {}", productId);
//                    return new RuntimeException("Product Not Found");
//                });
//
//        SubProducts subProduct = subProductsRepository.findById(subProductId)
//                .orElseThrow(() -> {
//                    log.error("SubProduct not found for subProductId: {}", subProductId);
//                    return new RuntimeException("Sub Product Not Found");
//                });
//
//        SchemeHierarchySelection hierarchySelection = schemeHierarchySelectionRepository.findById(schemeHierarchyId)
//                .orElseThrow(() -> {
//                    log.error("Scheme hierarchy not found for ID: {}", schemeHierarchyId);
//                    return new RuntimeException("Scheme hierarchy not found");
//                });
//
//        Set<String> allowedHierarchies = Arrays.stream(hierarchySelection.getSchemeHierarchyCombination().split("-"))
//                .map(String::toLowerCase)
//                .collect(Collectors.toSet());
//
//        log.debug("Allowed hierarchy levels: {}", allowedHierarchies);
//
//        List<Operators> operators = operatorRepository.findBySubProductId(subProduct.getId());
//        log.info("Found {} operators for subProductId: {}", operators.size(), subProduct.getId());
//
//        List<OperatorWithSlabsDto> operatorDtos = new ArrayList<>();
//
//        for (Operators operator : operators) {
//            log.debug("Processing operator: {} ({})", operator.getOperatorName(), operator.getOperatorCode());
//
//            List<SchemeRate> slabsList = schemeRateRepository
//                    .findBySchemeCodeAndOperatorName(schemeCode, operator.getOperatorName());
//
//            if (slabsList.isEmpty()) {
//                log.warn("No slabs defined for operator: {}", operator.getOperatorName());
//
//                OperatorWithSlabsDto operatorDto = new OperatorWithSlabsDto();
//                operatorDto.setOperatorCode(operator.getOperatorCode());
//                operatorDto.setOperatorName(operator.getOperatorName());
//                operatorDto.setSlabs(new ArrayList<>()); // empty list
//                operatorDtos.add(operatorDto);
//                continue; // skip rest
//            }
//
//            List<SlabRateDto> slabs = slabsList.stream().map(slab -> {
//                SlabRateDto dto = new SlabRateDto();
//                dto.setId(slab.getId());
//                dto.setRateType(slab.getRateType());
//                dto.setSlabType(slab.getSlabType());
//                dto.setFixedSlabValue(slab.getFixedSlabValue());
//                dto.setSlabStart(slab.getSlabStart());
//                dto.setSlabEnd(slab.getSlabEnd());
//
//                double factor = slab.getRateType() == RateType.PERCENTAGE ? 100.0 : 1.0;
//
//                dto.setGross(safeMultiply(slab.getGross(), factor));
//                dto.setIm(safeMultiply(slab.getIm(), factor));
//                dto.setEnterprise(allowedHierarchies.contains("enterprise") ? safeMultiply(slab.getEnterprise(), factor) : null);
//                dto.setWhitelabel(allowedHierarchies.contains("whitelabel") ? safeMultiply(slab.getWhitelabel(), factor) : null);
//                dto.setSuperDistributor(hasHierarchy("superdistributor", allowedHierarchies) ? safeMultiply(slab.getSuperDistributor(), factor) : null);
//                dto.setPartner(allowedHierarchies.contains("partner") ? safeMultiply(slab.getPartner(), factor) : null);
//                dto.setDistributor(allowedHierarchies.contains("distributor") ? safeMultiply(slab.getDistributor(), factor) : null);
//                dto.setRetailer(allowedHierarchies.contains("retailer") ? safeMultiply(slab.getRetailer(), factor) : null);
//
//                return dto;
//            }).collect(Collectors.toList());
//
//            log.debug("Added {} slabs for operator: {}", slabs.size(), operator.getOperatorName());
//
//            OperatorWithSlabsDto operatorDto = new OperatorWithSlabsDto();
//            operatorDto.setOperatorCode(operator.getOperatorCode());
//            operatorDto.setOperatorName(operator.getOperatorName());
//            operatorDto.setSlabs(slabs);
//
//            operatorDtos.add(operatorDto);
//        }
//
//        SchemeDetailResponseDto responseDto = new SchemeDetailResponseDto();
//        responseDto.setSchemeCode(scheme.getSchemeCode());
//        responseDto.setSchemeName(scheme.getSchemeName());
//        responseDto.setOperators(operatorDtos);
//        responseDto.setIsActive(scheme.getIsActive());
//        responseDto.setTdsRate(product.getTds());
//
//        log.info("Scheme detail response ready for schemeCode: {}", schemeCode);
//        return responseDto;
//    }

    @Override
    public SchemeDetailResponseDto getSchemeDetails(String schemeCode) {
        log.info("Fetching scheme details for schemeCode: {}", schemeCode);

        Scheme scheme = schemeRepository.findBySchemeCode(schemeCode)
                .orElseThrow(() -> {
                    log.error("Scheme not found for schemeCode: {}", schemeCode);
                    return new RuntimeException("Scheme not found");
                });

        log.debug("Scheme found: {}", scheme.getSchemeName());

        String[] parts = scheme.getSchemeCombinationHashCode().split("-");
        if (parts.length < 4) {
            log.error("Invalid scheme combination hash: {}", scheme.getSchemeCombinationHashCode());
            throw new RuntimeException("Invalid combination hash code format");
        }

        String productId = parts[0];
        String subProductId = parts[1];
        String schemeHierarchyId = parts[3];

        log.debug("Parsed combination hash - productId: {}, subProductId: {}, schemeHierarchyId: {}", productId, subProductId, schemeHierarchyId);

        Products product = productsRepository.findById(Short.valueOf(productId))
                .orElseThrow(() -> {
                    log.error("Product not found for productId: {}", productId);
                    return new RuntimeException("Product Not Found");
                });

        SubProducts subProduct = subProductsRepository.findById(subProductId)
                .orElseThrow(() -> {
                    log.error("SubProduct not found for subProductId: {}", subProductId);
                    return new RuntimeException("Sub Product Not Found");
                });

        SchemeHierarchySelection hierarchySelection = schemeHierarchySelectionRepository.findById(schemeHierarchyId)
                .orElseThrow(() -> {
                    log.error("Scheme hierarchy not found for ID: {}", schemeHierarchyId);
                    return new RuntimeException("Scheme hierarchy not found");
                });

        Set<String> allowedHierarchies = Arrays.stream(hierarchySelection.getSchemeHierarchyCombination().split("-"))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        log.debug("Allowed hierarchy levels: {}", allowedHierarchies);

        List<Operators> operators = operatorRepository.findBySubProductId(subProduct.getId());
        log.info("Found {} operators for subProductId: {}", operators.size(), subProduct.getId());

        List<OperatorWithSlabsDto> operatorDtos = new ArrayList<>();

        for (Operators operator : operators) {
            log.debug("Processing operator: {} ({})", operator.getOperatorName(), operator.getOperatorCode());

            List<SchemeRate> slabsList = schemeRateRepository
                    .findBySchemeCodeAndOperatorName(schemeCode, operator.getOperatorName());

            List<SlabRateDto> slabs;

            if (slabsList.isEmpty()) {
                log.warn("No slabs found for operator: {}. Creating default empty slab.", operator.getOperatorName());
                slabs = List.of(createEmptySlab(allowedHierarchies));
            } else {
                slabs = slabsList.stream().map(slab -> {
                    SlabRateDto dto = new SlabRateDto();
                    dto.setId(slab.getId());
                    dto.setRateType(slab.getRateType());
                    dto.setSlabType(slab.getSlabType());
                    dto.setFixedSlabValue(slab.getFixedSlabValue());
                    dto.setSlabStart(slab.getSlabStart());
                    dto.setSlabEnd(slab.getSlabEnd());

                    boolean isPercentage = slab.getRateType() == RateType.PERCENTAGE;
                    double factor = isPercentage ? 100.0 : 1.0;

                    dto.setGross(applyRounding(slab.getGross(), factor, isPercentage));
                    dto.setIm(applyRounding(slab.getIm(), factor, isPercentage));
                    dto.setEnterprise(allowedHierarchies.contains("enterprise") ? applyRounding(slab.getEnterprise(), factor, isPercentage) : null);
                    dto.setWhitelabel(allowedHierarchies.contains("whitelabel") ? applyRounding(slab.getWhitelabel(), factor, isPercentage) : null);
                    dto.setSuperDistributor(hasHierarchy("superdistributor", allowedHierarchies) ? applyRounding(slab.getSuperDistributor(), factor, isPercentage) : null);
                    dto.setPartner(allowedHierarchies.contains("partner") ? applyRounding(slab.getPartner(), factor, isPercentage) : null);
                    dto.setDistributor(allowedHierarchies.contains("distributor") ? applyRounding(slab.getDistributor(), factor, isPercentage) : null);
                    dto.setRetailer(allowedHierarchies.contains("retailer") ? applyRounding(slab.getRetailer(), factor, isPercentage) : null);

                    return dto;
                }).collect(Collectors.toList());

                log.debug("Added {} slabs for operator: {}", slabs.size(), operator.getOperatorName());
            }

            OperatorWithSlabsDto operatorDto = new OperatorWithSlabsDto();
            operatorDto.setOperatorCode(operator.getOperatorCode());
            operatorDto.setOperatorName(operator.getOperatorName());
            operatorDto.setSlabs(slabs);

            operatorDtos.add(operatorDto);
        }

        SchemeDetailResponseDto responseDto = new SchemeDetailResponseDto();
        responseDto.setSchemeCode(scheme.getSchemeCode());
        responseDto.setSchemeName(scheme.getSchemeName());
        responseDto.setOperators(operatorDtos);
        responseDto.setIsActive(scheme.getIsActive());
        responseDto.setTdsRate(product.getTds());

        log.info("Scheme detail response ready for schemeCode: {}", schemeCode);
        return responseDto;
    }

    private Double applyRounding(Double value, double factor, boolean round) {
        if (value == null) return null;
        double result = value * factor;
        return round ? roundToThreeDecimalPlaces(result) : result;
    }

    private Double roundToThreeDecimalPlaces(Double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private SlabRateDto createEmptySlab(Set<String> allowedHierarchies) {
        log.debug("Creating empty default slab for allowed hierarchies: {}", allowedHierarchies);
        SlabRateDto dto = new SlabRateDto();
        dto.setId(null);
        dto.setRateType(RateType.FLAT);
        dto.setSlabType(SlabType.FIXED);
        dto.setFixedSlabValue(0.0);
        dto.setSlabStart(0.0);
        dto.setSlabEnd(0.0);
        dto.setGross(0.0);
        dto.setIm(0.0);
        dto.setEnterprise(allowedHierarchies.contains("enterprise") ? 0.0 : null);
        dto.setWhitelabel(allowedHierarchies.contains("whitelabel") ? 0.0 : null);
        dto.setSuperDistributor(hasHierarchy("superdistributor", allowedHierarchies) ? 0.0 : null);
        dto.setPartner(allowedHierarchies.contains("partner") ? 0.0 : null);
        dto.setDistributor(allowedHierarchies.contains("distributor") ? 0.0 : null);
        dto.setRetailer(allowedHierarchies.contains("retailer") ? 0.0 : null);
        return dto;
    }

    private boolean hasHierarchy(String key, Set<String> allowed) {
        boolean result = allowed.contains(key) || allowed.contains(key.replace("_", ""));
        log.debug("Checking hierarchy '{}' in allowed set: {}", key, result);
        return result;
    }

    private Double safeMultiply(Double value, Double factor) {
        if (value == null) return null;
        double result = value * factor;
        log.trace("Multiplying value: {} with factor: {} = {}", value, factor, result);
        return result;
    }

    private boolean doubleEquals(double a, double b) {
        return Math.abs(roundToThreeDecimalPlaces(a) - roundToThreeDecimalPlaces(b)) < 0.001;
    }

    @Override
    public List<SchemeRate> createSchemeRates(SchemeRateRequestDTO requestDTO) {
        log.info("Creating scheme rates for schemeCode: {}", requestDTO.getSchemeCode());

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

            log.debug("Processing slab #{}: start={}, end={}, rateType={}", i + 1, slab.getSlabStart(), slab.getSlabEnd(), slab.getRateType());

            double im = slab.getIm() == null ? 0.0 : slab.getIm();
            double enterprise = slab.getEnterprise() == null ? 0.0 : slab.getEnterprise();
            double whitelabel = slab.getWhitelabel() == null ? 0.0 : slab.getWhitelabel();
            double superDistributor = slab.getSuperDistributor() == null ? 0.0 : slab.getSuperDistributor();
            double partner = slab.getPartner() == null ? 0.0 : slab.getPartner();
            double distributor = slab.getDistributor() == null ? 0.0 : slab.getDistributor();
            double retailer = slab.getRetailer() == null ? 0.0 : slab.getRetailer();

            double gross = slab.getGross();

            log.debug("Validating gross for slab #{}: gross={}, sum={}, operator={}",
                    i + 1, gross,
                    im + enterprise + whitelabel + superDistributor + partner + distributor + retailer,
                    requestDTO.getOperatorName());

            validateGrossMatch(
                    gross, im, enterprise, whitelabel, superDistributor,
                    partner, distributor, retailer, "slab #" + (i + 1)
            );

            if (slab.getRateType() == RateType.PERCENTAGE) {
                schemeRate.setGross(roundToThreeDecimalPlaces(gross / 100));
                schemeRate.setIm(roundToThreeDecimalPlaces(im / 100));
                schemeRate.setEnterprise(roundToThreeDecimalPlaces(enterprise / 100));
                schemeRate.setWhitelabel(roundToThreeDecimalPlaces(whitelabel / 100));
                schemeRate.setSuperDistributor(roundToThreeDecimalPlaces(superDistributor / 100));
                schemeRate.setPartner(roundToThreeDecimalPlaces(partner / 100));
                schemeRate.setDistributor(roundToThreeDecimalPlaces(distributor / 100));
                schemeRate.setRetailer(roundToThreeDecimalPlaces(retailer / 100));
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

            log.debug("Added slab #{} for operator {}: {}", i + 1, requestDTO.getOperatorCode(), schemeRate);
        }

        List<SchemeRate> savedRates = schemeRateRepository.saveAll(schemeRates);
        log.info("Successfully saved {} scheme slabs for schemeCode: {}", savedRates.size(), requestDTO.getSchemeCode());

        return savedRates;
    }

    @Override
    @Transactional
    public SchemeDetailResponseDto updateSchemeRates(SchemeSlabUpdateRequestDTO requestDto) {
        log.info("Updating scheme slabs for schemeCode: {}", requestDto.getSchemeCode());

        for (OperatorWithSlabsDto operator : requestDto.getOperators()) {
            log.debug("Processing operator: {} ({})", operator.getOperatorName(), operator.getOperatorCode());

            for (SlabRateDto slabDto : operator.getSlabs()) {
                SchemeRate slab;
                String slabIdentifier = slabDto.getId() != null ? "ID: " + slabDto.getId() : "NEW";

                if (slabDto.getId() != null) {
                    log.debug("Updating existing slab with ID: {}", slabDto.getId());
                    slab = schemeRateRepository.findById(slabDto.getId())
                            .orElseThrow(() -> {
                                log.error("Slab not found for ID: {}", slabDto.getId());
                                return new RuntimeException("Slab not found for ID: " + slabDto.getId());
                            });
                    slab.setCreatedBy(slabDto.getCreatedBy());
                } else {
                    log.debug("Creating new slab for schemeCode: {}", requestDto.getSchemeCode());
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

                log.debug("Validating gross for {}: gross={}, sum={}", slabIdentifier,
                        gross, im + enterprise + whitelabel + superDistributor + partner + distributor + retailer);

                validateGrossMatch(
                        gross, im, enterprise, whitelabel, superDistributor,
                        partner, distributor, retailer,
                        "slab " + slabIdentifier
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
                log.debug("Saved slab: {}", slabIdentifier);
            }
        }

        log.info("Successfully updated slabs. Fetching latest scheme detail for: {}", requestDto.getSchemeCode());
        return getSchemeDetails(requestDto.getSchemeCode());
    }

    private double safeValue(Double val) {
        if (val == null) {
            log.debug("safeValue() received null, defaulting to 0.0");
            return 0.0;
        }
        return val;
    }

    @Override
    @Transactional(readOnly = true)
    public SchemeRateProductResponseDto getProductDetails(String schemeCode) {
        log.info("Fetching product details for schemeCode: {}", schemeCode);

        Scheme scheme = schemeRepository.findBySchemeCode(schemeCode)
                .orElseThrow(() -> {
                    log.error("Scheme not found for schemeCode: {}", schemeCode);
                    return new RuntimeException("Scheme not found for scheme code: " + schemeCode);
                });

        String[] parts = scheme.getSchemeCombinationHashCode().split("-");
        if (parts.length < 2) {
            log.error("Invalid combination hash code format: {}", scheme.getSchemeCombinationHashCode());
            throw new RuntimeException("Invalid scheme combination hash code format for scheme code: " + schemeCode);
        }

        short productId;
        try {
            productId = Short.parseShort(parts[0]);
            log.debug("Parsed productId: {} from hash: {}", productId, scheme.getSchemeCombinationHashCode());
        } catch (NumberFormatException e) {
            log.error("Invalid product ID in combination hash: {}", scheme.getSchemeCombinationHashCode(), e);
            throw new RuntimeException("Invalid product ID in hash code for scheme code: " + schemeCode, e);
        }

        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product not found for ID: {}", productId);
                    return new RuntimeException("Product not found for ID: " + productId);
                });

        SchemeRateProductResponseDto dto = mapToProductResponseDto(product);
        log.info("Successfully fetched product details for schemeCode: {}", schemeCode);
        return dto;
    }

    private SchemeRateProductResponseDto mapToProductResponseDto(Products product) {
        log.debug("Mapping product entity to DTO for productCode: {}", product.getProductCode());

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

        log.debug("Mapped DTO: {}", dto);
        return dto;
    }

    private void validateGrossMatch(double gross, double im, double enterprise, double whitelabel,
                                    double superDistributor, double partner, double distributor,
                                    double retailer, String slabLabel) {

        double sum = im + enterprise + whitelabel + superDistributor + partner + distributor + retailer;

        if (!doubleEquals(gross, sum)) {
            throw new RuntimeException(String.format(
                    "Error in %s: Sum of all shares must be equal to Gross. Expected: %.3f, Found: %.3f",
                    slabLabel, gross, sum));
        }
    }



}
