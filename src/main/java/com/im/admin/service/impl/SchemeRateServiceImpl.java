package com.im.admin.service.impl;


import com.im.admin.dto.SchemeRateRequestDTO;
import com.im.admin.dto.SchemeRateSlabDTO;
import com.im.admin.entities.Scheme;
import com.im.admin.entities.SchemeHierarchySelection;
import com.im.admin.entities.SchemeRate;
import com.im.admin.entities.enums.RateType;
import com.im.admin.repositories.SchemeHierarchySelectionRepository;
import com.im.admin.repositories.SchemeRateRepository;
import com.im.admin.repositories.SchemeRepository;
import com.im.admin.request.OperatorWithSlabsDto;
import com.im.admin.request.SlabRateDto;
import com.im.admin.response.SchemeDetailResponseDto;
import com.im.admin.service.SchemeRateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
                dto.setRateType(slab.getRateType());
                dto.setSlabType(slab.getSlabType());
                dto.setFixedSlabValue(slab.getFixedSlabValue());
                dto.setSlabStart(slab.getSlabStart());
                dto.setSlabEnd(slab.getSlabEnd());
                dto.setGross(slab.getGross());
                dto.setIm(slab.getIm());

                // Conditionally include only those hierarchy levels present in SchemeHierarchySelection
                if (allowedHierarchies.contains("enterprise")) dto.setEnterprise(slab.getEnterprise());
                if (allowedHierarchies.contains("whitelabel")) dto.setWhitelabel(slab.getWhitelabel());
                if (allowedHierarchies.contains("superdistributor") || allowedHierarchies.contains("super_distributor")) dto.setSuperDistributor(slab.getSuperDistributor());
                if (allowedHierarchies.contains("partner")) dto.setPartner(slab.getPartner());
                if (allowedHierarchies.contains("distributor")) dto.setDistributor(slab.getDistributor());
                if (allowedHierarchies.contains("retailer")) dto.setRetailer(slab.getRetailer());

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

            double totalDistributed = im + enterprise + whitelabel + superDistributor + partner + distributor + retailer;
            double gross = slab.getGross();

            if (!Objects.equals(gross, totalDistributed)) {
                throw new IllegalArgumentException("Error in slab #" + (i + 1) + ": Sum of all fields must be exactly equal to Gross (Expected: " + gross + ", Found: " + totalDistributed + ").");
            }

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

            schemeRate.setCreatedBy("admin");
            schemeRates.add(schemeRate);
        }

        return schemeRateRepository.saveAll(schemeRates);
    }

}
