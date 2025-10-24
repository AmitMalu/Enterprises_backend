package com.im.admin.service.impl.reports;

import com.im.admin.dto.DownlineBalanceDto;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.DownLineBalanceReportService;
import com.im.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class DownLineBalanceReportImpl implements DownLineBalanceReportService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private RetailerPrepaidAmountRepository retailerPrepaidAmountRepository;

    @Autowired
    private RetailerPostpaidAmountRepository retailerPostpaidAmountRepository;

    @Autowired
    private DistributorPrepaidAmountRepository distributorPrepaidAmountRepository;

    @Autowired
    private DistributorPostpaidAmountRepository distributorPostpaidAmountRepository;

    @Autowired
    private PartnerPrepaidAmountRepository partnerPrepaidAmountRepository;

    @Autowired
    private PartnerPostpaidAmountRepository partnerPostpaidAmountRepository;

    @Autowired
    private SuperDistributorPrepaidAmountRepository superDistributorPrepaidAmountRepository;

    @Autowired
    private SuperDistributorPostpaidAmountRepository superDistributorPostpaidAmountRepository;

    @Autowired
    private WhiteLabelPrepaidAmountRepository whiteLabelPrepaidAmountRepository;

    @Autowired
    private WhiteLabelPostpaidAmountRepository whiteLabelPostpaidAmountRepository;

    @Autowired
    private EnterprisesPrepaidAmountRepository enterprisesPrepaidAmountRepository;

    @Autowired
    private EnterprisesPostpaidAmountRepository enterprisesPostpaidAmountRepository;

    @Override
    public CommonResponse<Page<DownlineBalanceDto>> getDownlineBalanceReport(
            String hierarchyId,
            String userId,
            String name,
            String parentAccount,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minPrepaidBalance,
            BigDecimal maxPrepaidBalance,
            BigDecimal minPostpaidBalance,
            BigDecimal maxPostpaidBalance,
            int page,
            int size
    ) {
        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);

        List<String> childEntities = hierarchyChildren.get(hierarchyType);
        if (childEntities == null || childEntities.isEmpty()) {
            throw new RuntimeException("No downline entities for type: " + hierarchyType);
        }

        List<DownlineBalanceDto> result = new ArrayList<>();

        for (String childEntity : childEntities) {
            switch (childEntity) {


                case "enterprise" -> enterprisesRepository.findByParentAccount(hierarchyId)
                        .forEach(et -> {
                            BigDecimal prepaid = enterprisesPrepaidAmountRepository
                                    .findByEnterprise_EnterpriseId(et.getEnterpriseId())
                                    .map(EnterprisesPrepaidAmount::getAvailableBalance)
                                    .orElse(BigDecimal.ZERO);

                            BigDecimal postpaid = enterprisesPostpaidAmountRepository
                                    .findByEnterprise_EnterpriseId(et.getEnterpriseId())
                                    .map(EnterprisesPostpaidAmount::getAvailableBalance)
                                    .orElse(BigDecimal.ZERO);

                            result.add(new DownlineBalanceDto(
                                    et.getEnterpriseId(),
                                    et.getName(),
                                    et.getParentAccount(),
                                    prepaid,
                                    postpaid,
                                    et.getCreatedOn()
                            ));
                        });


                case "white_label" -> whiteLabelRepository.findByParentAccount(hierarchyId)
                        .forEach(wl -> {
                            BigDecimal prepaid = whiteLabelPrepaidAmountRepository
                                    .findByWhiteLabelId_WhiteLabelId(wl.getWhiteLabelId())
                                    .map(WhiteLabelPrepaidAmount::getAvailableBalance)
                                    .orElse(BigDecimal.ZERO);

                            BigDecimal postpaid = whiteLabelPostpaidAmountRepository
                                    .findByWhiteLabelId_WhiteLabelId(wl.getWhiteLabelId())
                                    .map(WhiteLabelPostpaidAmount::getAvailableBalance)
                                    .orElse(BigDecimal.ZERO);

                            result.add(new DownlineBalanceDto(
                                    wl.getWhiteLabelId(),
                                    wl.getName(),
                                    wl.getParentAccount(),
                                    prepaid,
                                    postpaid,
                                    wl.getCreatedOn()
                            ));
                        });


                case "super_distributor" -> superDistributorRepository.findByParentAccount(hierarchyId)
                        .forEach(sd -> {
                            BigDecimal prepaid = superDistributorPrepaidAmountRepository
                                    .findBySuperDistributorId_SuperDistributorId(sd.getSuperDistributorId())
                                    .map(SuperDistributorPrepaidAmount::getAvailableBalance)
                                    .orElse(BigDecimal.ZERO);

                            BigDecimal postpaid = superDistributorPostpaidAmountRepository
                                    .findBySuperDistributorId_SuperDistributorId(sd.getSuperDistributorId())
                                    .map(SuperDistributorPostpaidAmount::getAvailableBalance)
                                    .orElse(BigDecimal.ZERO);

                            result.add(new DownlineBalanceDto(
                                    sd.getSuperDistributorId(),
                                    sd.getName(),
                                    sd.getParentAccount(),
                                    prepaid,
                                    postpaid,
                                    sd.getCreatedOn()
                            ));
                        });


                case "distributor" -> distributorRepository.findByParentAccount(hierarchyId)
                        .forEach(d -> {
                            BigDecimal prepaid = distributorPrepaidAmountRepository
                                    .findByDistributorId_DistributorId(d.getDistributorId())
                                    .map(DistributorPrepaidAmount::getAvailableBalance)
                                    .orElse(BigDecimal.ZERO);

                            BigDecimal postpaid = distributorPostpaidAmountRepository
                                    .findByDistributorId_DistributorId(d.getDistributorId())
                                    .map(DistributorPostpaidAmount::getAvailableBalance)
                                    .orElse(BigDecimal.ZERO);

                            result.add(new DownlineBalanceDto(
                                    d.getDistributorId(),
                                    d.getName(),
                                    d.getParentAccount(),
                                    prepaid,
                                    postpaid,
                                    d.getCreatedOn()
                            ));
                        });


                case "partner" -> partnerRepository.findByParentAccount(hierarchyId)
                        .forEach(p -> {
                            BigDecimal prepaid = partnerPrepaidAmountRepository
                                    .findByPartnerId_PartnerId(p.getPartnerId())
                                    .map(PartnerPrepaidAmount::getAvailableBalance)
                                    .orElse(BigDecimal.ZERO);

                            BigDecimal postpaid = partnerPostpaidAmountRepository
                                    .findByPartnerId_PartnerId(p.getPartnerId())
                                    .map(PartnerPostpaidAmount::getAvailableBalance)
                                    .orElse(BigDecimal.ZERO);

                            result.add(new DownlineBalanceDto(
                                    p.getPartnerId(),
                                    p.getName(),
                                    p.getParentAccount(),
                                    prepaid,
                                    postpaid,
                                    p.getCreatedOn()
                            ));
                        });


                case "retailer" -> retailerRepository.findByParentAccount(hierarchyId)
                        .forEach(r -> {
                            BigDecimal prepaid = retailerPrepaidAmountRepository
                                    .findByRetailerId_RetailerId(r.getRetailerId())
                                    .map(RetailerPrepaidAmount::getAvailableBalance)
                                    .orElse(BigDecimal.ZERO);

                            BigDecimal postpaid = retailerPostpaidAmountRepository
                                    .findByRetailerId_RetailerId(r.getRetailerId())
                                    .map(RetailerPostpaidAmount::getAvailableBalance)
                                    .orElse(BigDecimal.ZERO);

                            result.add(new DownlineBalanceDto(
                                    r.getRetailerId(),
                                    r.getName(),
                                    r.getParentAccount(),
                                    prepaid,
                                    postpaid,
                                    r.getCreatedOn()
                            ));
                        });
            }
        }


        List<DownlineBalanceDto> filteredList = result.stream()
                .filter(u -> userId == null || (u.getUserId() != null && u.getUserId().toLowerCase().contains(userId.toLowerCase())))
                .filter(u -> name == null || (u.getName() != null && u.getName().toLowerCase().contains(name.toLowerCase())))
                .filter(u -> parentAccount == null || (u.getParentAccount() != null && u.getParentAccount().toLowerCase().contains(parentAccount.toLowerCase())))
                .filter(u -> fromDate == null || (u.getCreatedOn() != null && !u.getCreatedOn().toLocalDate().isBefore(fromDate)))
                .filter(u -> toDate == null || (u.getCreatedOn() != null && !u.getCreatedOn().toLocalDate().isAfter(toDate)))
                .filter(u -> minPrepaidBalance == null || (u.getPrepaidBalance() != null && u.getPrepaidBalance().compareTo(minPrepaidBalance) >= 0))
                .filter(u -> maxPrepaidBalance == null || (u.getPrepaidBalance() != null && u.getPrepaidBalance().compareTo(maxPrepaidBalance) <= 0))
                .filter(u -> minPostpaidBalance == null || (u.getPostpaidBalance() != null && u.getPostpaidBalance().compareTo(minPostpaidBalance) >= 0))
                .filter(u -> maxPostpaidBalance == null || (u.getPostpaidBalance() != null && u.getPostpaidBalance().compareTo(maxPostpaidBalance) <= 0))
                .toList();

        // --- Pagination with Sorting ---
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdOn"));
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredList.size());
        List<DownlineBalanceDto> paginatedList = (start >= filteredList.size()) ? Collections.emptyList() : filteredList.subList(start, end);

        Page<DownlineBalanceDto> pagedResponse = new PageImpl<>(paginatedList, pageable, filteredList.size());

        return new CommonResponse<>(200, "Success", pagedResponse);
    }


    private static final Map<String, List<String>> hierarchyChildren = Map.of(
            "ET", List.of("enterprise", "white_label", "super_distributor", "partner", "distributor", "retailer"),
            "WL", List.of("super_distributor", "partner", "distributor", "retailer"),
            "SD", List.of("partner", "distributor", "retailer"),
            "PT", List.of("distributor", "retailer"),
            "DT", List.of("retailer")
    );
}
