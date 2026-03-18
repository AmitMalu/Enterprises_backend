package com.im.admin.strategy;


import com.im.admin.dto.DashboardResponseDto;
import com.im.admin.dto.WalletBalanceResponseDto;
import com.im.admin.entities.*;
        import com.im.admin.repositories.*;
        import com.im.admin.request.DashboardTransactionRequest;
import com.im.admin.response.*;
        import com.im.admin.service.DashboardService;
import com.im.admin.service.MonthlyTransactionVolumeProjection;
import com.im.admin.service.ProductAmountProjection;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
        import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {
    @Autowired
    private WhiteLabelPostpaidAmountRepository whiteLabelPostpaidAmountRepository;
    @Autowired
    private WhiteLabelPrepaidAmountRepository whiteLabelPrepaidAmountRepository;
    @Autowired
    private SuperDistributorPostpaidAmountRepository superDistributorPostpaidAmountRepository;

    @Autowired
    private SuperDistributorPrepaidAmountRepository superDistributorPrepaidAmountRepository;

    @Autowired
    private DashboardQueryRepository dashboardQueryRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private RetailerUserRepository retailerUserRepository;

    @Autowired
    private BankDetailsRepository bankDetailsRepository;

    @Autowired
    private RetailerPrepaidTransactionDetailsRepository retailerPrepaidTransactionDetailsRepository;

    @Autowired
    private RetailerPrepaidAmountRepository retailerPrepaidAmountRepository;

    @Autowired
    private RetailerPostpaidAmountRepository retailerPostpaidAmountRepository;


    @Autowired
    private RetailerPostpaidTransactionDetailsRepository retailerPostpaidTransactionDetailsRepository;

    @Autowired
    private FundTransferRepository fundTransferRepository;

    @Autowired
    private FundRequestRepository fundRequestRepository;

    @Autowired
    private SuperDistributorPostpaidTransactionDetailsRepository sdPostpaidTransactionDetailsRepository;

    @Autowired
    private SuperDistributorPrepaidTransactionDetailsRepository sdPrepaidTransactionDetailsRepository;

    @Autowired
    private PartnerPrepaidTransactionDetailsRepository ptPrepaidTransactionDetailsRepository;

    @Autowired
    private PartnerPostpaidTransactionDetailsRepository ptPostpaidTransactionDetailsRepository;

    @Autowired
    private DistributorPostpaidTransactionDetailsRepository dtPostpaidTransactionDetailsRepository;

    @Autowired
    private DistributorPrepaidTransactionDetailsRepository dtPrepaidTransactionDetailsRepository;

    @Autowired
    private DistributorPrepaidAmountRepository distributorPrepaidAmountRepository;

    @Autowired
    private DistributorPostpaidAmountRepository distributorPostpaidAmountRepository;

    @Autowired
    private PartnerPrepaidAmountRepository partnerPrepaidAmountRepository;

    @Autowired
    private PartnerPostpaidAmountRepository partnerPostpaidAmountRepository;

    @Autowired
    private EnterprisesPostpaidAmountRepository enterprisesPostpaidAmountRepository;

    @Autowired
    private EnterprisesPrepaidAmountRepository enterprisesPrepaidAmountRepository;

    @Autowired
    private AdminPrepaidAmountRepository adminPrepaidAmountRepository;


    @Autowired
    private AdminPostpaidAmountRepository adminPostpaidAmountRepository;


    @Override
    public DashboardResponseDto getAdminDashboard() {
        Map<String, Object> sums = dashboardQueryRepository.fetchTotalTransactionSums();

        BigDecimal totalPrepaid = sumBigDecimals(
                sums.get("retailer_prepaid"),
                sums.get("distributor_prepaid"),
                sums.get("partner_prepaid"),
                sums.get("super_distributor_prepaid"),
                sums.get("enterprises_prepaid"),
                sums.get("white_label_prepaid")
        );

        BigDecimal totalPostpaid = sumBigDecimals(
                sums.get("retailer_postpaid"),
                sums.get("distributor_postpaid"),
                sums.get("partner_postpaid"),
                sums.get("super_distributor_postpaid"),
                sums.get("enterprises_postpaid"),
                sums.get("white_label_postpaid")
        );

        BigDecimal totalTransaction = totalPrepaid.add(totalPostpaid);

        // Sum of pending prepaid transactions
        BigDecimal pendingPrepaid = sumBigDecimals(
                sums.get("retailer_prepaid_pending"),
                sums.get("distributor_prepaid_pending"),
                sums.get("partner_prepaid_pending"),
                sums.get("super_distributor_prepaid_pending"),
                sums.get("enterprises_prepaid_pending"),
                sums.get("white_label_prepaid_pending")
        );

        // Sum of pending postpaid transactions
        BigDecimal pendingPostpaid = sumBigDecimals(
                sums.get("retailer_postpaid_pending"),
                sums.get("distributor_postpaid_pending"),
                sums.get("partner_postpaid_pending"),
                sums.get("super_distributor_postpaid_pending"),
                sums.get("enterprises_postpaid_pending"),
                sums.get("white_label_postpaid_pending")
        );

        // Total pending transactions (Prepaid + Postpaid)
        BigDecimal pendingTransaction = pendingPrepaid.add(pendingPostpaid);

        //Pending onboarding count
        Long pendingOnboarding = dashboardQueryRepository.fetchPendingOnboarding();

        DashboardResponseDto response = new DashboardResponseDto();
        response.setTotalTransaction(totalTransaction);
        response.setPendingTransaction(pendingTransaction);
        response.setPendingOnboarding(pendingOnboarding);

        return response;
    }

    private BigDecimal sumBigDecimals(Object... values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Object value : values) {
            if (value instanceof BigDecimal) {
                sum = sum.add((BigDecimal) value);
            } else if (value instanceof Number) {
                sum = sum.add(BigDecimal.valueOf(((Number) value).doubleValue()));
            } else if (value != null) {
                sum = sum.add(new BigDecimal(value.toString()));
            }
        }
        return sum;
    }

    @Override
    public WalletBalanceResponseDto getWalletBalances(String hierarchyId) {
        BigDecimal prepaid = commonMethods.getCurrentBalance(hierarchyId, "PREPAID");
        BigDecimal postpaid = commonMethods.getCurrentBalance(hierarchyId, "POSTPAID");

        return new WalletBalanceResponseDto(prepaid, postpaid);
    }

    @Override
    public MyProfileResponseDto getUserProfile(String hierarchyId) {
        MyProfileResponseDto response = null;
        String type = commonMethods.extractHierarchyType(hierarchyId);
        switch (type) {
            case "AD" -> {
                return adminRepository.findById(hierarchyId)
                        .map(this::mapToUserDetails)
                        .orElseThrow(() -> new RuntimeException("Admin profile not found"));
            }
            case "EN" -> {
                return enterprisesRepository.findByEnterpriseId(hierarchyId)
                        .map(this::mapToUserDetails)
                        .orElseThrow(() -> new RuntimeException("Enterprise profile not found"));  // Enterprises
            }
            case "WL" -> {
                return whiteLabelRepository.findByWhiteLabelId(hierarchyId)
                        .map(this::mapToUserDetails)
                        .orElseThrow(() -> new RuntimeException("WhiteLabel profile not found"));  // White Label
            }
            case "SD" -> {
                return superDistributorRepository.findBySuperDistributorId(hierarchyId)
                        .map(this::mapToUserDetails)
                        .orElseThrow(() -> new RuntimeException("SuperDistributor profile not found"));  // Super Distributor
            }
            case "PT" -> {
                return partnerRepository.findByPartnerId(hierarchyId)
                        .map(this::mapToUserDetails)
                        .orElseThrow(() -> new RuntimeException("Partner profile not found"));  // Partner
            }
            case "DT" -> {
                return distributorRepository.findByDistributorId(hierarchyId)
                        .map(this::mapToUserDetails)
                        .orElseThrow(() -> new RuntimeException("Distributor profile not found"));

            }
            case "RT" -> {
                return retailerRepository.findByRetailerId(hierarchyId)
                        .map(this::mapToUserDetails)
                        .orElseThrow(() -> new RuntimeException("Retailer profile not found"));  // Retailer

            }
            case "RU" -> {
                return retailerUserRepository.findById(hierarchyId)
                        .map(this::mapToUserDetails)
                        .orElseThrow(() -> new RuntimeException("Retailer User profile not found"));
            }
            default -> throw new RuntimeException("Unsupported hierarchy type: " + type);
        }
    }

    private MyProfileResponseDto mapToUserDetails(Object entity) {
        MyProfileResponseDto response = new MyProfileResponseDto();
        InfoDto info = new InfoDto();
        AddressDto address = new AddressDto();
        OutletAddressDto outlet = new OutletAddressDto();
        KycDocsDto docsDto = new KycDocsDto();

        if (entity instanceof Admin ad) {
            info.setName(ad.getName());
            info.setMemberId(ad.getId());
            info.setEmail(ad.getEmailId());
            info.setMobileNumber(ad.getMobileNumber());
            info.setStatus(ad.getStatus());
        }

        if (entity instanceof Retailer r) {

            Optional<UserDetails> userDetailsOpt = userDetailsRepository.findFirstByHierarchyId(r.getRetailerId());
            UserDetails userDetails = userDetailsOpt.get();

            info.setName(r.getName());
            info.setMemberId(r.getRetailerId());
            info.setMobileNumber(r.getMobileNumber());
            info.setEmail(r.getEmailId());
            info.setDob(r.getDob() != null ? r.getDob().toString() : null);
            info.setOutletName(r.getOutletAddress().getOutletName());
            info.setIsBlocked(userDetails.getIsBlocked());

            address.setBuilding(r.getAddress().getAddress());
            address.setCity(r.getAddress().getCity());
            address.setState(r.getAddress().getState());
            address.setPinCode(r.getAddress().getZipCode());


            outlet.setOutletBuilding(r.getOutletAddress().getAddress());
            outlet.setOutletCity(r.getOutletAddress().getCity());
            outlet.setOutletState(r.getOutletAddress().getState());
            outlet.setOutletPinCode(r.getOutletAddress().getZipCode());

            // Bank Accounts (multiple)
            List<BanksDetails> banksDetailssList = bankDetailsRepository.findByHierarchyId(r.getRetailerId());
            List<MyBanksDto> myBanksDtoList = banksDetailssList.stream()
                    .map(bank -> {
                        MyBanksDto banksDto = new MyBanksDto();
                        banksDto.setId(bank.getId());
                        banksDto.setBankName(bank.getBankName());
                        banksDto.setAccountName(bank.getBankHolderName() != null ? bank.getBankHolderName() : r.getName());
                        banksDto.setAccountNo(bank.getBankAccountNumber());
                        banksDto.setIFCSCode(bank.getIfscCode());
                        banksDto.setVerified(bank.getIsVerified());
                        return banksDto;
                    })
                    .collect(Collectors.toList());

            response.setMyBanksList(myBanksDtoList);

            docsDto.setAadhaarCardFrontImage(r.getAadhaarCardFrontImage());
            docsDto.setAadhaarCardBackImage(r.getAadhaarCardBackImage());
            docsDto.setPanCardImage(r.getPanCardImage());
            docsDto.setShopImage(r.getShopImage());
            docsDto.setBusinessProofImage(r.getBusinessProofImage());

        }

        if (entity instanceof RetailerUser ru) {
            info.setName(ru.getName());
            info.setMemberId(ru.getRetailerUserId());
            info.setEmail(ru.getEmailId());
            info.setMobileNumber(ru.getMobileNumber());
            // info.setStatus(ru.get);
        }

        if (entity instanceof Distributor dt) {

            Optional<UserDetails> userDetailsOpt = userDetailsRepository.findFirstByHierarchyId(dt.getDistributorId());
            UserDetails userDetails = userDetailsOpt.get();

            info.setName(dt.getName());
            info.setMemberId(dt.getDistributorId());
            info.setMobileNumber(dt.getMobileNumber());
            info.setEmail(dt.getEmailId());
            info.setDob(dt.getDob() != null ? dt.getDob().toString() : null);
            info.setOutletName(dt.getOutletAddress().getOutletName());
            info.setIsBlocked(userDetails.getIsBlocked());

            address.setBuilding(dt.getAddress().getAddress());
            address.setCity(dt.getAddress().getCity());
            address.setState(dt.getAddress().getState());
            address.setPinCode(dt.getAddress().getZipCode());

            outlet.setOutletBuilding(dt.getOutletAddress().getAddress());
            outlet.setOutletCity(dt.getOutletAddress().getCity());
            outlet.setOutletState(dt.getOutletAddress().getState());
            outlet.setOutletPinCode(dt.getOutletAddress().getZipCode());

            // Bank Accounts (multiple)
            List<BanksDetails> banksDetailssList = bankDetailsRepository.findByHierarchyId(dt.getDistributorId());
            List<MyBanksDto> myBanksDtoList = banksDetailssList.stream()
                    .map(bank -> {
                        MyBanksDto banksDto = new MyBanksDto();
                        banksDto.setId(bank.getId());
                        banksDto.setBankName(bank.getBankName());
                        banksDto.setAccountName(bank.getBankHolderName());
                        banksDto.setAccountNo(bank.getBankAccountNumber());
                        banksDto.setIFCSCode(bank.getIfscCode());
                        banksDto.setVerified(bank.getIsVerified());
                        return banksDto;
                    })
                    .collect(Collectors.toList());

            response.setMyBanksList(myBanksDtoList);

            docsDto.setAadhaarCardFrontImage(dt.getAadhaarCardFrontImage());
            docsDto.setAadhaarCardBackImage(dt.getAadhaarCardBackImage());
            docsDto.setPanCardImage(dt.getPanCardImage());
            docsDto.setShopImage(dt.getShopImage());
            docsDto.setBusinessProofImage(dt.getBusinessProofImage());

        }

        if (entity instanceof Partner pt) {

            Optional<UserDetails> userDetailsOpt = userDetailsRepository.findFirstByHierarchyId(pt.getPartnerId());
            UserDetails userDetails = userDetailsOpt.get();

            info.setName(pt.getName());
            info.setMemberId(pt.getPartnerId());
            info.setMobileNumber(pt.getMobileNumber());
            info.setEmail(pt.getEmailId());
            info.setDob(pt.getDob() != null ? pt.getDob().toString() : null);
            info.setOutletName(pt.getOutletAddress().getOutletName());
            info.setIsBlocked(userDetails.getIsBlocked());

            address.setBuilding(pt.getAddress().getAddress());
            address.setCity(pt.getAddress().getCity());
            address.setState(pt.getAddress().getState());
            address.setPinCode(pt.getAddress().getZipCode());

            outlet.setOutletBuilding(pt.getOutletAddress().getAddress());
            outlet.setOutletCity(pt.getOutletAddress().getCity());
            outlet.setOutletState(pt.getOutletAddress().getState());
            outlet.setOutletPinCode(pt.getOutletAddress().getZipCode());

            // Bank Accounts (multiple)
            List<BanksDetails> banksDetailssList = bankDetailsRepository.findByHierarchyId(pt.getPartnerId());
            List<MyBanksDto> myBanksDtoList = banksDetailssList.stream()
                    .map(bank -> {
                        MyBanksDto banksDto = new MyBanksDto();
                        banksDto.setId(bank.getId());
                        banksDto.setBankName(bank.getBankName());
                        banksDto.setAccountName(bank.getBankHolderName());
                        banksDto.setAccountNo(bank.getBankAccountNumber());
                        banksDto.setIFCSCode(bank.getIfscCode());
                        banksDto.setVerified(bank.getIsVerified());
                        return banksDto;
                    })
                    .collect(Collectors.toList());

            response.setMyBanksList(myBanksDtoList);

            docsDto.setAadhaarCardFrontImage(pt.getAadhaarCardFrontImage());
            docsDto.setAadhaarCardBackImage(pt.getAadhaarCardBackImage());
            docsDto.setPanCardImage(pt.getPanCardImage());
            docsDto.setShopImage(pt.getShopImage());
            docsDto.setBusinessProofImage(pt.getBusinessProofImage());

        }

        if (entity instanceof SuperDistributor sd) {

            Optional<UserDetails> userDetailsOpt = userDetailsRepository.findFirstByHierarchyId(sd.getSuperDistributorId());
            UserDetails userDetails = userDetailsOpt.get();

            info.setName(sd.getName());
            info.setMemberId(sd.getSuperDistributorId());
            info.setMobileNumber(sd.getMobileNumber());
            info.setEmail(sd.getEmailId());
            info.setDob(sd.getDob() != null ? sd.getDob().toString() : null);
            info.setOutletName(sd.getOutletAddress().getOutletName());
            info.setIsBlocked(userDetails.getIsBlocked());

            address.setBuilding(sd.getAddress().getAddress());
            address.setCity(sd.getAddress().getCity());
            address.setState(sd.getAddress().getState());
            address.setPinCode(sd.getAddress().getZipCode());

            outlet.setOutletBuilding(sd.getOutletAddress().getAddress());
            outlet.setOutletCity(sd.getOutletAddress().getCity());
            outlet.setOutletState(sd.getOutletAddress().getState());
            outlet.setOutletPinCode(sd.getOutletAddress().getZipCode());

            // Bank Accounts (multiple)
            List<BanksDetails> banksDetailssList = bankDetailsRepository.findByHierarchyId(sd.getSuperDistributorId());
            List<MyBanksDto> myBanksDtoList = banksDetailssList.stream()
                    .map(bank -> {
                        MyBanksDto banksDto = new MyBanksDto();
                        banksDto.setId(bank.getId());
                        banksDto.setBankName(bank.getBankName());
                        banksDto.setAccountName(bank.getBankHolderName());
                        banksDto.setAccountNo(bank.getBankAccountNumber());
                        banksDto.setIFCSCode(bank.getIfscCode());
                        banksDto.setVerified(bank.getIsVerified());
                        return banksDto;
                    })
                    .collect(Collectors.toList());

            response.setMyBanksList(myBanksDtoList);

            docsDto.setAadhaarCardFrontImage(sd.getAadhaarCardFrontImage());
            docsDto.setAadhaarCardBackImage(sd.getAadhaarCardBackImage());
            docsDto.setPanCardImage(sd.getPanCardImage());
            docsDto.setShopImage(sd.getShopImage());
            docsDto.setBusinessProofImage(sd.getBusinessProofImage());

        }

        if (entity instanceof WhiteLabels wl) {

            Optional<UserDetails> userDetailsOpt = userDetailsRepository.findFirstByHierarchyId(wl.getWhiteLabelId());
            UserDetails userDetails = userDetailsOpt.get();

            info.setName(wl.getName());
            info.setMemberId(wl.getWhiteLabelId());
            info.setMobileNumber(wl.getMobileNumber());
            info.setEmail(wl.getEmailId());
            info.setDob(wl.getDob() != null ? wl.getDob().toString() : null);
            info.setOutletName(wl.getOutletAddress().getOutletName());
            info.setIsBlocked(userDetails.getIsBlocked());

            address.setBuilding(wl.getAddress().getAddress());
            address.setCity(wl.getAddress().getCity());
            address.setState(wl.getAddress().getState());
            address.setPinCode(wl.getAddress().getZipCode());

            outlet.setOutletBuilding(wl.getOutletAddress().getAddress());
            outlet.setOutletCity(wl.getOutletAddress().getCity());
            outlet.setOutletState(wl.getOutletAddress().getState());
            outlet.setOutletPinCode(wl.getOutletAddress().getZipCode());

            // Bank Accounts (multiple)
            List<BanksDetails> banksDetailssList = bankDetailsRepository.findByHierarchyId(wl.getWhiteLabelId());
            List<MyBanksDto> myBanksDtoList = banksDetailssList.stream()
                    .map(bank -> {
                        MyBanksDto banksDto = new MyBanksDto();
                        banksDto.setId(bank.getId());
                        banksDto.setBankName(bank.getBankName());
                        banksDto.setAccountName(bank.getBankHolderName());
                        banksDto.setAccountNo(bank.getBankAccountNumber());
                        banksDto.setIFCSCode(bank.getIfscCode());
                        banksDto.setVerified(bank.getIsVerified());
                        return banksDto;
                    })
                    .collect(Collectors.toList());

            response.setMyBanksList(myBanksDtoList);

            docsDto.setAadhaarCardFrontImage(wl.getAadhaarCardFrontImage());
            docsDto.setAadhaarCardBackImage(wl.getAadhaarCardBackImage());
            docsDto.setPanCardImage(wl.getPanCardImage());
            docsDto.setShopImage(wl.getShopImage());
            docsDto.setBusinessProofImage(wl.getBusinessProofImage());

        }

        if (entity instanceof Enterprises et) {

            Optional<UserDetails> userDetailsOpt = userDetailsRepository.findFirstByHierarchyId(et.getEnterpriseId());
            UserDetails userDetails = userDetailsOpt.get();

            info.setName(et.getName());
            info.setMemberId(et.getEnterpriseId());
            info.setMobileNumber(et.getMobileNumber());
            info.setEmail(et.getEmailId());
            info.setDob(et.getDob() != null ? et.getDob().toString() : null);
            info.setOutletName(et.getOutletAddress().getOutletName());
            info.setIsBlocked(userDetails.getIsBlocked());

            address.setBuilding(et.getAddress().getAddress());
            address.setCity(et.getAddress().getCity());
            address.setState(et.getAddress().getState());
            address.setPinCode(et.getAddress().getZipCode());

            outlet.setOutletBuilding(et.getOutletAddress().getAddress());
            outlet.setOutletCity(et.getOutletAddress().getCity());
            outlet.setOutletState(et.getOutletAddress().getState());
            outlet.setOutletPinCode(et.getOutletAddress().getZipCode());

            // Bank Accounts (multiple)
            List<BanksDetails> banksDetailssList = bankDetailsRepository.findByHierarchyId(et.getEnterpriseId());
            List<MyBanksDto> myBanksDtoList = banksDetailssList.stream()
                    .map(bank -> {
                        MyBanksDto banksDto = new MyBanksDto();
                        banksDto.setId(bank.getId());
                        banksDto.setBankName(bank.getBankName());
                        banksDto.setAccountName(bank.getBankHolderName());
                        banksDto.setAccountNo(bank.getBankAccountNumber());
                        banksDto.setIFCSCode(bank.getIfscCode());
                        banksDto.setVerified(bank.getIsVerified());
                        return banksDto;
                    })
                    .collect(Collectors.toList());

            response.setMyBanksList(myBanksDtoList);

            docsDto.setAadhaarCardFrontImage(et.getAadhaarCardFrontImage());
            docsDto.setAadhaarCardBackImage(et.getAadhaarCardBackImage());
            docsDto.setPanCardImage(et.getPanCardImage());
            docsDto.setShopImage(et.getShopImage());
            docsDto.setBusinessProofImage(et.getBusinessProofImage());

        }

        response.setInfo(info);
        response.setAddress(address);
        response.setOutletAddress(outlet);
        response.setKycDocs(docsDto);

        return response;
    }

    @Override
    public Map<String, Object> getDashboardTransactions(DashboardTransactionRequest request) {

        LocalDate fromDate = StringUtils.hasText(request.getFromDate())
                ? LocalDate.parse(request.getFromDate())
                : null;

        LocalDate toDate = StringUtils.hasText(request.getToDate())
                ? LocalDate.parse(request.getToDate())
                : null;

        String type = commonMethods.extractHierarchyType(request.getHierarchyId());

        switch (type) {

            case "AD" -> {

                adminRepository.findById(request.getHierarchyId())
                        .orElseThrow(() -> new RuntimeException("Admin not found"));

                List<Map<String, Object>> barGraphData =
                        getAdBarGraphData(
                                request.getHierarchyId(),
                                fromDate,
                                toDate
                        );

                //  Total product volume
                BigDecimal productVolume = barGraphData.stream()
                        .map(m -> (BigDecimal) m.get("volume"))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                //  Transaction Volume (Total Amount)
                BigDecimal txnVolume =
                        getAdTransactionVolume(
                                request.getHierarchyId(),
                                fromDate,
                                toDate
                        );

                Long pendingCount =
                        getAdPendingTransactionCount(
                                request.getHierarchyId(),
                                fromDate,
                                toDate
                        );

                //  Line Graph (Product Wise TXN Count)
                List<Map<String, Object>> lineGraphData =
                        getAdLineGraphData(
                                request.getHierarchyId(),
                                fromDate,
                                toDate
                        );


                Map<String, Long> onboarding = getOnboardingCounts();

                Map<String, BigDecimal> balances =
                        getAdTotalBalances(request.getHierarchyId());

                Long totalRetailers =
                        (long) getRetailersUnderHierarchy(request.getHierarchyId())
                                .stream()
                                .distinct()
                                .count();

                // ADD CARDS
                List<Map<String, Object>> cards = List.of(
                        Map.of(
                                "label", "TRANSACTION VOLUME",
                                "value", txnVolume.toPlainString(),
                                "prefix", "₹"
                        ),

                        Map.of("label", "PRODUCT VOLUME", "value", productVolume.toPlainString(),"prefix", "₹"),
                        Map.of("label", "PENDING TRANSACTION", "value", pendingCount.toString()),
                        Map.of("label", "APPROVED USERS", "value", onboarding.get("APPROVED").toString()),
                        Map.of("label", "REJECTED USERS", "value", onboarding.get("REJECTED").toString()),
                        Map.of("label", "PENDING USERS", "value", onboarding.get("PENDING").toString()),
                        Map.of("label", "PREPAID BALANCE", "value", balances.get("PREPAID").toPlainString(), "prefix", "₹"),
                        Map.of("label", "POSTPAID BALANCE", "value", balances.get("POSTPAID").toPlainString(), "prefix", "₹"),
                        Map.of("label", "TRANSACTING RETAILER COUNT", "value", totalRetailers.toString())
                );

                return Map.of(
                        "profile", Map.of(),
                        "cards", cards,
                        "barGraphData", barGraphData,
                        "lineGraphData", lineGraphData
                );
            }

            case "EN" -> {

                Enterprises enterprise = enterprisesRepository
                        .findByEnterpriseId(request.getHierarchyId())
                        .orElseThrow(() -> new RuntimeException("Enterprise not found"));

                // ===== Wallet balances (EN) =====
                EnterprisesPrepaidAmount enterprisePrepaidAmount =
                        enterprisesPrepaidAmountRepository
                                .findByEnterprise_EnterpriseId(request.getHierarchyId())
                                .orElse(null);

                EnterprisesPostpaidAmount enterprisePostpaidAmount =
                        enterprisesPostpaidAmountRepository
                                .findByEnterprise_EnterpriseId(request.getHierarchyId())
                                .orElse(null);

                Map<String, Object> profile = Map.of(
                        "image", enterprise.getShopImage() != null
                                ? enterprise.getShopImage()
                                : "",
                        "name", enterprise.getOutletAddress().getOutletName(),
                        "hierarchyId", enterprise.getEnterpriseId(),
                        "prepaid", enterprisePrepaidAmount != null
                                ? enterprisePrepaidAmount.getAvailableBalance()
                                : BigDecimal.ZERO,
                        "postpaid", enterprisePostpaidAmount != null
                                ? enterprisePostpaidAmount.getAvailableBalance()
                                : BigDecimal.ZERO
                );

                BigDecimal txnVolume =
                        getTransactionVolume(request.getHierarchyId(), fromDate, toDate);

                Long pendingTxnCount =
                        getPendingTransactionCount(request.getHierarchyId(), fromDate, toDate);

                Long retailerCount =
                        (long) getRetailersUnderHierarchy(request.getHierarchyId()).size();

                BigDecimal fundTransferred =
                        getFundTransferred(request.getHierarchyId(), fromDate, toDate);

                BigDecimal fundReceived =
                        getFundReceived(request.getHierarchyId(), fromDate, toDate);

                BigDecimal commissionEarned =
                        getCommissionEarned(request.getHierarchyId(), type, fromDate, toDate);

                //garph data
                List<Map<String, Object>> graphData =
                        getLast4MonthsGraphData(request.getHierarchyId());

                List<Map<String, Object>> tableData =
                        getTop5ByHierarchy(
                                request.getHierarchyId(),
                                type,
                                fromDate,
                                toDate
                        );

                List<Map<String, Object>> cards = List.of(
                        Map.of(
                                "label", "TRANSACTION VOLUME",
                                "value", txnVolume.toPlainString(),
                                "prefix", "₹"
                        ),

                        Map.of("label", "PENDING TRANSACTION", "value", pendingTxnCount),
                        Map.of("label", "RETAILERS CREATED", "value", retailerCount),
                        Map.of("label", "FUND TRANSFERRED", "value", fundTransferred,"prefix","₹"),
                        Map.of("label", "FUND RECEIVED", "value", fundReceived,"prefix","₹"),
                        Map.of("label", "COMMISSION EARNED", "value", commissionEarned,"prefix","₹")
                );

                return Map.of(
                        "profile", profile,
                        "cards", cards,
                        "graphData", graphData,
                        "tableData", tableData
                );
            }

            case "WL" -> {

                WhiteLabels whiteLabel = whiteLabelRepository
                        .findByWhiteLabelId(request.getHierarchyId())
                        .orElseThrow(() -> new RuntimeException("WhiteLabel not found"));


                // ===== Wallet balances (WL) =====
                WhiteLabelPrepaidAmount whiteLabelPrepaidAmount =
                        whiteLabelPrepaidAmountRepository
                                .findByWhiteLabelId_WhiteLabelId(request.getHierarchyId())
                                .orElse(null);

                WhiteLabelPostpaidAmount whiteLabelPostpaidAmount =
                        whiteLabelPostpaidAmountRepository
                                .findByWhiteLabelId_WhiteLabelId(request.getHierarchyId())
                                .orElse(null);

                Map<String, Object> profile = Map.of(
                        "image", whiteLabel.getShopImage() != null
                                ? whiteLabel.getShopImage()
                                : "",
                        "name", whiteLabel.getOutletAddress().getOutletName(),
                        "hierarchyId", whiteLabel.getWhiteLabelId(),
                        "prepaid", whiteLabelPrepaidAmount != null
                                ? whiteLabelPrepaidAmount.getAvailableBalance()
                                : BigDecimal.ZERO,
                        "postpaid", whiteLabelPostpaidAmount != null
                                ? whiteLabelPostpaidAmount.getAvailableBalance()
                                : BigDecimal.ZERO
                );

                BigDecimal txnVolume =
                        getTransactionVolume(request.getHierarchyId(), fromDate, toDate);

                Long pendingTxnCount =
                        getPendingTransactionCount(request.getHierarchyId(), fromDate, toDate);

                Long retailerCount =
                        (long) getRetailersUnderHierarchy(request.getHierarchyId()).size();

                BigDecimal fundTransferred =
                        getFundTransferred(request.getHierarchyId(), fromDate, toDate);

                BigDecimal fundReceived =
                        getFundReceived(request.getHierarchyId(), fromDate, toDate);

                BigDecimal commissionEarned =
                        getCommissionEarned(request.getHierarchyId(), type, fromDate, toDate);

                //garph data
                List<Map<String, Object>> graphData =
                        getLast4MonthsGraphData(request.getHierarchyId());

                List<Map<String, Object>> tableData =
                        getTop5ByHierarchy(
                                request.getHierarchyId(),
                                type,
                                fromDate,
                                toDate
                        );

                List<Map<String, Object>> cards = List.of(
                        Map.of("label", "TRANSACTION VOLUME", "value", txnVolume, "prefix","₹"),
                        Map.of("label", "PENDING TRANSACTION", "value", pendingTxnCount),
                        Map.of("label", "RETAILERS CREATED", "value", retailerCount),
                        Map.of("label", "FUND TRANSFERRED", "value", fundTransferred,"prefix","₹"),
                        Map.of("label", "FUND RECEIVED", "value", fundReceived,"prefix","₹"),
                        Map.of("label", "COMMISSION EARNED", "value", commissionEarned,"prefix","₹")
                );

                return Map.of(
                        "profile", profile,
                        "cards", cards,
                        "graphData", graphData,
                        "tableData", tableData
                );
            }

            case "SD" -> {
                SuperDistributor superdistributor = superDistributorRepository.findBySuperDistributorId(request.getHierarchyId())
                        .orElseThrow(() -> new RuntimeException("SuperDistributor not found"));

                // ===== Wallet balances =====
                SuperDistributorPrepaidAmount superDistributorPrepaidAmount = superDistributorPrepaidAmountRepository
                        .findBySuperDistributorId_SuperDistributorId(request.getHierarchyId())
                        .orElse(null);

                SuperDistributorPostpaidAmount superDistributorPostpaidAmount = superDistributorPostpaidAmountRepository
                        .findBySuperDistributorId_SuperDistributorId(request.getHierarchyId())
                        .orElse(null);


                Map<String, Object> profile = Map.of(
                        "image", superdistributor.getShopImage() != null ? superdistributor.getShopImage() : "",
                        "name", superdistributor.getOutletAddress().getOutletName(),
                        "retailerId", superdistributor.getSuperDistributorId(),
                        "prepaid", superDistributorPrepaidAmount != null ? Objects.requireNonNull(superDistributorPostpaidAmount).getAvailableBalance() : BigDecimal.ZERO,
                        "postpaid", superDistributorPostpaidAmount != null ? superDistributorPostpaidAmount.getAvailableBalance() : BigDecimal.ZERO
                );

                // ===== CARDS 1.Transaction Volume
//                BigDecimal txnVolume =                      //old
//                        getSdTransactionVolume(
//                                request.getHierarchyId(),
//                                fromDate,
//                                toDate
//                        );
//
//                //2.Cards pending txn
//                Long pendingTxnCount =                     //old
//                        getSdPendingTransactionCount(
//                                request.getHierarchyId(),
//                                fromDate,
//                                toDate
//                        );

//                //5 cards Retailers count
//                Long retailerCount =
//                        getSdRetailerCount(request.getHierarchyId());

                BigDecimal txnVolume =
                        getTransactionVolume(request.getHierarchyId(), fromDate, toDate);

                Long pendingTxnCount =
                        getPendingTransactionCount(request.getHierarchyId(), fromDate, toDate);

                Long retailerCount =
                        (long) getRetailersUnderHierarchy(request.getHierarchyId()).size();


                // 3. Fund Transferred (generic for all hierarchies)
                BigDecimal fundTransferred = getFundTransferred(
                        request.getHierarchyId(),
                        fromDate,
                        toDate
                );

                // 4. Fund Received (generic for all hierarchies)
                BigDecimal fundReceived = getFundReceived(
                        request.getHierarchyId(),
                        fromDate,
                        toDate
                );

                //6.Commission Earned
                BigDecimal commissionEarned =
                        getCommissionEarned(
                                request.getHierarchyId(),
                                type,
                                fromDate,
                                toDate
                        );

//                //garph data                           //old
//                List<Map<String, Object>> graphData =
//                        getSdLast4MonthsGraphData(request.getHierarchyId());

                //garph data
                List<Map<String, Object>> graphData =
                        getLast4MonthsGraphData(request.getHierarchyId());


//                List<Map<String, Object>> tableData =      //old
//                        getSdTop5Partners(
//                                request.getHierarchyId(),
//                                fromDate,
//                                toDate
//                        );

                List<Map<String, Object>> tableData =
                        getTop5ByHierarchy(
                                request.getHierarchyId(),
                                type,
                                fromDate,
                                toDate
                        );


                List<Map<String, Object>> cards = List.of(
                        Map.of(
                                "label", "TRANSACTION VOLUME",
                                "value", txnVolume,
                                "prefix","₹"
                        ),
                        Map.of(
                                "label", "PENDING TRANSACTION",
                                "value", pendingTxnCount
                        ),
                        Map.of(
                                "label", "RETAILERS CREATED",
                                "value", retailerCount
                        ),
                        Map.of(
                                "label", "FUND TRANSFERRED",
                                "value", fundTransferred,
                                "prefix","₹"
                        ),
                        Map.of(
                                "label", "FUND RECEIVED",
                                "value", fundReceived,
                                "prefix","₹"
                        ),
                        Map.of(
                                "label", "COMMISSION EARNED",
                                "value", commissionEarned.toString(),
                                "prefix","₹"
                        )

                );

                return Map.of(
                        "profile", profile,
                        "cards", cards,
                        "graphData", graphData,
                        "tableData", tableData
                );


            }

            case "PT" -> {

                Partner partner = partnerRepository
                        .findByPartnerId(request.getHierarchyId())
                        .orElseThrow(() -> new RuntimeException("Partner not found"));

                // ===== Wallet balances (PT) =====
                PartnerPrepaidAmount partnerPrepaidAmount =
                        partnerPrepaidAmountRepository
                                .findByPartnerId_PartnerId(request.getHierarchyId())
                                .orElse(null);

                PartnerPostpaidAmount partnerPostpaidAmount =
                        partnerPostpaidAmountRepository
                                .findByPartnerId_PartnerId(request.getHierarchyId())
                                .orElse(null);

                Map<String, Object> profile = Map.of(
                        "image", partner.getShopImage() != null
                                ? partner.getShopImage()
                                : "",
                        "name", partner.getOutletAddress().getOutletName(),
                        "hierarchyId", partner.getPartnerId(),
                        "prepaid", partnerPrepaidAmount != null
                                ? partnerPrepaidAmount.getAvailableBalance()
                                : BigDecimal.ZERO,
                        "postpaid", partnerPostpaidAmount != null
                                ? partnerPostpaidAmount.getAvailableBalance()
                                : BigDecimal.ZERO
                );

                BigDecimal txnVolume =
                        getTransactionVolume(request.getHierarchyId(), fromDate, toDate);

                Long pendingTxnCount =
                        getPendingTransactionCount(request.getHierarchyId(), fromDate, toDate);

                Long retailerCount =
                        (long) getRetailersUnderHierarchy(request.getHierarchyId()).size();

                BigDecimal fundTransferred =
                        getFundTransferred(request.getHierarchyId(), fromDate, toDate);

                BigDecimal fundReceived =
                        getFundReceived(request.getHierarchyId(), fromDate, toDate);

                BigDecimal commissionEarned =
                        getCommissionEarned(request.getHierarchyId(), type, fromDate, toDate);

                //garph data
                List<Map<String, Object>> graphData =
                        getLast4MonthsGraphData(request.getHierarchyId());

                List<Map<String, Object>> tableData =
                        getTop5ByHierarchy(
                                request.getHierarchyId(),
                                type,
                                fromDate,
                                toDate
                        );


                List<Map<String, Object>> cards = List.of(
                        Map.of("label", "TRANSACTION VOLUME", "value", txnVolume, "prefix","₹"),
                        Map.of("label", "PENDING TRANSACTION", "value", pendingTxnCount),
                        Map.of("label", "RETAILERS CREATED", "value", retailerCount),
                        Map.of("label", "FUND TRANSFERRED", "value", fundTransferred, "prefix","₹"),
                        Map.of("label", "FUND RECEIVED", "value", fundReceived,"prefix","₹"),
                        Map.of("label", "COMMISSION EARNED", "value", commissionEarned,"prefix","₹"));

                return Map.of(
                        "profile", profile,
                        "cards", cards,
                        "graphData", graphData,
                        "tableData", tableData
                );
            }

            case "DT" -> {
                Distributor distributor = distributorRepository.findByDistributorId(request.getHierarchyId())
                        .orElseThrow(() -> new RuntimeException("Distributor not found"));


                // ===== Wallet balances (DT) =====
                DistributorPrepaidAmount distributorPrepaidAmount =
                        distributorPrepaidAmountRepository
                                .findByDistributorId_DistributorId(request.getHierarchyId())
                                .orElse(null);

                DistributorPostpaidAmount distributorPostpaidAmount =
                        distributorPostpaidAmountRepository
                                .findByDistributorId_DistributorId(request.getHierarchyId())
                                .orElse(null);


                Map<String, Object> profile = Map.of(
                        "image", distributor.getShopImage() != null
                                ? distributor.getShopImage()
                                : "",
                        "name", distributor.getOutletAddress().getOutletName(),
                        "hierarchyId", distributor.getDistributorId(),
                        "prepaid", distributorPrepaidAmount != null
                                ? distributorPrepaidAmount.getAvailableBalance()
                                : BigDecimal.ZERO,
                        "postpaid", distributorPostpaidAmount != null
                                ? distributorPostpaidAmount.getAvailableBalance()
                                : BigDecimal.ZERO
                );

                BigDecimal txnVolume =
                        getTransactionVolume(request.getHierarchyId(), fromDate, toDate);

                Long pendingTxnCount =
                        getPendingTransactionCount(request.getHierarchyId(), fromDate, toDate);

                Long retailerCount =
                        (long) getRetailersUnderHierarchy(request.getHierarchyId()).size();

                BigDecimal fundTransferred =
                        getFundTransferred(request.getHierarchyId(), fromDate, toDate);

                BigDecimal fundReceived =
                        getFundReceived(request.getHierarchyId(), fromDate, toDate);

                BigDecimal commissionEarned =
                        getCommissionEarned(request.getHierarchyId(), type, fromDate, toDate);

                //garph data
                List<Map<String, Object>> graphData =
                        getLast4MonthsGraphData(request.getHierarchyId());


                List<Map<String, Object>> tableData =
                        getTop5ByHierarchy(
                                request.getHierarchyId(),
                                type,
                                fromDate,
                                toDate
                        );


                List<Map<String, Object>> cards = List.of(
                        Map.of("label", "TRANSACTION VOLUME", "value", txnVolume, "prefix","₹"),
                        Map.of("label", "PENDING TRANSACTION", "value", pendingTxnCount),
                        Map.of("label", "RETAILERS CREATED", "value", retailerCount),
                        Map.of("label", "FUND TRANSFERRED", "value", fundTransferred, "prefix","₹"),
                        Map.of("label", "FUND RECEIVED", "value", fundReceived, "prefix","₹"),
                        Map.of("label", "COMMISSION EARNED", "value", commissionEarned, "prefix","₹")
                );

                return Map.of(
                        "profile", profile,
                        "cards", cards,
                        "graphData", graphData,
                        "tableData", tableData
                );
            }

            // ================= RETAILER DASHBOARD =================
            case "RT" -> {

                // ===== Parse retailer =====
                Retailer retailer = retailerRepository.findByRetailerId(request.getHierarchyId())
                        .orElseThrow(() -> new RuntimeException("Retailer not found"));

                // ===== Last 5 transactions =====
                List<DashboardTransactionDto> prepaid =
                        retailerPrepaidTransactionDetailsRepository
                                .fetchDashboardTransactions(request.getHierarchyId());

                List<DashboardTransactionDto> postpaid =
                        retailerPostpaidTransactionDetailsRepository
                                .fetchDashboardTransactions(request.getHierarchyId());

                List<DashboardTransactionDto> transactions =
                        Stream.concat(prepaid.stream(), postpaid.stream())
                                .sorted(
                                        Comparator
                                                .comparing(DashboardTransactionDto::getTransactionDate, Comparator.reverseOrder())
                                                .thenComparing(DashboardTransactionDto::getTransactionTime, Comparator.reverseOrder())
                                )
                                .limit(5)
                                .toList();

                // ===== Table data mapping =====
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yy");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

                List<Map<String, String>> tableData = transactions.stream()
                        .map(t -> {
                            Map<String, String> map = new HashMap<>();

                            map.put(
                                    "dateTime",
                                    (t.getTransactionDate() != null && t.getTransactionTime() != null)
                                            ? t.getTransactionDate().format(dateFormatter) + " " +
                                            t.getTransactionTime().format(timeFormatter)
                                            : ""
                            );

                            map.put("wallet", t.getWalletType() != null ? t.getWalletType() : "");
                            map.put("product", t.getProductName() != null ? t.getProductName() : "");
                            map.put("status", t.getTranStatus() != null ? t.getTranStatus() : "");
                            map.put("amount", t.getAmount() != null ? t.getAmount().toPlainString() : "0");

                            return map;
                        })
                        .toList();


                // ===== Wallet balances =====
                RetailerPrepaidAmount prepaidAmount =
                        retailerPrepaidAmountRepository
                                .findByRetailerId_RetailerId(request.getHierarchyId())
                                .orElse(null);

                RetailerPostpaidAmount postpaidAmount =
                        retailerPostpaidAmountRepository
                                .findByRetailerId_RetailerId(request.getHierarchyId())
                                .orElse(null);

                Map<String, Object> profile = Map.of(
                        "image", retailer.getShopImage() != null ? retailer.getShopImage() : "",
                        "name", retailer.getOutletAddress().getOutletName(),
                        "retailerId", retailer.getRetailerId(),
                        "prepaid", prepaidAmount != null ? prepaidAmount.getAvailableBalance() : BigDecimal.ZERO,
                        "postpaid", postpaidAmount != null ? postpaidAmount.getAvailableBalance() : BigDecimal.ZERO
                );

                // ===== TRANSACTION VOLUME (TXN% + SUCCESS + date range) =====
                BigDecimal prepaidVolume =
                        retailerPrepaidTransactionDetailsRepository
                                .sumTransactionVolume(
                                        request.getHierarchyId(),
                                        fromDate,
                                        toDate
                                );

                BigDecimal postpaidVolume =
                        retailerPostpaidTransactionDetailsRepository
                                .sumTransactionVolume(
                                        request.getHierarchyId(),
                                        fromDate,
                                        toDate
                                );

                BigDecimal transactionVolume =
                        prepaidVolume.add(postpaidVolume);

                //Commission Earned
                BigDecimal commissionEarned =
                        getCommissionEarned(
                                request.getHierarchyId(),
                                type,
                                fromDate,
                                toDate
                        );

                //Hierarchy Count
                HierarchyCountResponse hierarchyCountResponse = getHierarchyCounts(request.getHierarchyId());


                Long prepaidPending =
                        retailerPrepaidTransactionDetailsRepository
                                .countPendingTransactions(
                                        request.getHierarchyId(),
                                        fromDate,
                                        toDate
                                );

                Long postpaidPending =
                        retailerPostpaidTransactionDetailsRepository
                                .countPendingTransactions(
                                        request.getHierarchyId(),
                                        fromDate,
                                        toDate
                                );

                Long pendingTransactions =
                        (prepaidPending != null ? prepaidPending : 0)
                                + (postpaidPending != null ? postpaidPending : 0);


                // ===== LAST 4 MONTHS FROM DATE =====
                LocalDate fromDatee = LocalDate.now()
                        .minusMonths(3)
                        .withDayOfMonth(1);

// ===== PREPAID DATA =====
                List<MonthlyTransactionVolumeProjection> prepaidData =
                        retailerPrepaidTransactionDetailsRepository
                                .getLast4MonthsTransactionVolume(
                                        request.getHierarchyId(),
                                        fromDatee
                                );

// ===== POSTPAID DATA =====
                List<MonthlyTransactionVolumeProjection> postpaidData =
                        retailerPostpaidTransactionDetailsRepository
                                .getLast4MonthsTransactionVolume(
                                        request.getHierarchyId(),
                                        fromDatee
                                );

// ===== MERGE MONTH WISE (PREPAID + POSTPAID) =====
                Map<String, BigDecimal> monthVsAmount = new HashMap<>();

                for (MonthlyTransactionVolumeProjection p : prepaidData) {
                    monthVsAmount.merge(
                            p.getMonth(),
                            p.getValue(),
                            BigDecimal::add
                    );
                }

                for (MonthlyTransactionVolumeProjection p : postpaidData) {
                    monthVsAmount.merge(
                            p.getMonth(),
                            p.getValue(),
                            BigDecimal::add
                    );
                }

// ===== BUILD GRAPH DATA (ENSURE 4 MONTHS, ZERO IF MISSING) =====
                List<Map<String, Object>> graphData = new ArrayList<>();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

                for (int i = 3; i >= 0; i--) {
                    LocalDate monthDate = LocalDate.now().minusMonths(i);
                    String monthKey = monthDate.format(formatter);

                    graphData.add(
                            Map.of(
                                    "month", monthKey,
                                    "value", monthVsAmount.getOrDefault(monthKey, BigDecimal.ZERO)
                            )
                    );
                }


                List<Map<String, String>> cards = List.of(
                        Map.of(
                                "label", "TRANSACTION VOLUME",
                                "value", transactionVolume.toPlainString(),
                                        "prefix","₹"
                        ),
                        Map.of(
                                "label", "COMMISSION EARNED",
                                "value", commissionEarned.toPlainString(),
                                "prefix","₹"
                        ),
                        Map.of(
                                "label", "PENDING TRANSACTION",
                                "value", pendingTransactions.toString()
                        ),
                        Map.of(
                                "label", "TOTAL NETWORK",
                                "value", String.valueOf(hierarchyCountResponse.getTotalCount())
                        ),
                        Map.of(
                                "label", "ACTIVE NETWORK",
                                "value", String.valueOf(hierarchyCountResponse.getTotalCount())
                        )
                );

                // ===== PREPAID =====
                List<ProductAmountProjection> prepaidProductData =
                        retailerPrepaidTransactionDetailsRepository
                                .getThisMonthProductWiseAmount(request.getHierarchyId());

                // ===== POSTPAID =====
                List<ProductAmountProjection> postpaidProductData =
                        retailerPostpaidTransactionDetailsRepository
                                .getThisMonthProductWiseAmount(request.getHierarchyId());

                // ===== MERGE PRODUCT WISE =====
                Map<String, BigDecimal> productVsAmount = new HashMap<>();

                for (ProductAmountProjection p : prepaidProductData) {
                    productVsAmount.merge(
                            p.getProductName(),
                            p.getAmount(),
                            BigDecimal::add
                    );
                }

                for (ProductAmountProjection p : postpaidProductData) {
                    productVsAmount.merge(
                            p.getProductName(),
                            p.getAmount(),
                            BigDecimal::add
                    );
                }

                // ===== TOTAL AMOUNT =====
                BigDecimal totalAmount = productVsAmount.values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // ===== PIE DATA =====
                List<Map<String, Object>> servicePieData = new ArrayList<>();

                for (Map.Entry<String, BigDecimal> entry : productVsAmount.entrySet()) {

                    BigDecimal percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                            ? entry.getValue()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(totalAmount, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    servicePieData.add(
                            Map.of(
                                    "name", entry.getKey(),
                                    "value", percentage
                            )
                    );
                }


                // ===== SINGLE RETURN =====
                return Map.of(
                        "profile", profile,
                        "cards", cards,
                        "tableData", tableData,
                        "graphData", graphData,
                        "servicePieData", servicePieData
                );
            }


            default -> throw new RuntimeException("Unsupported hierarchy type: " + type);
        }
    }

    public BigDecimal getSdTransactionVolume(
            String sdId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        // SD -> Partner
        List<String> partnerIds =
                partnerRepository.findPartnerIdsBySd(sdId);
        if (partnerIds.isEmpty()) return BigDecimal.ZERO;

        // Partner -> Distributor
        List<String> distributorIds =
                distributorRepository.findDistributorIdsByPartners(partnerIds);
        if (distributorIds.isEmpty()) return BigDecimal.ZERO;

        // Distributor -> Retailer
        List<String> retailerIds =
                retailerRepository.findRetailerIdsByDistributors(distributorIds);
        if (retailerIds.isEmpty()) return BigDecimal.ZERO;

        BigDecimal prepaid =
                retailerPrepaidTransactionDetailsRepository.sumTransactionVolumeForRetailers(
                        retailerIds, fromDate, toDate);

        BigDecimal postpaid =
                retailerPostpaidTransactionDetailsRepository.sumTransactionVolumeForRetailers(
                        retailerIds, fromDate, toDate);

        return prepaid.add(postpaid);
    }

    public Long getSdPendingTransactionCount(
            String sdId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        // SD -> Partner
        List<String> partnerIds =
                partnerRepository.findPartnerIdsBySd(sdId);
        if (partnerIds.isEmpty()) return 0L;

        // Partner -> Distributor
        List<String> distributorIds =
                distributorRepository.findDistributorIdsByPartners(partnerIds);
        if (distributorIds.isEmpty()) return 0L;

        // Distributor -> Retailer
        List<String> retailerIds =
                retailerRepository.findRetailerIdsByDistributors(distributorIds);
        if (retailerIds.isEmpty()) return 0L;

        Long prepaidPending =
                retailerPrepaidTransactionDetailsRepository
                        .countPendingTransactionsForRetailers(
                                retailerIds, fromDate, toDate);

        Long postpaidPending =
                retailerPostpaidTransactionDetailsRepository
                        .countPendingTransactionsForRetailers(
                                retailerIds, fromDate, toDate);

        return prepaidPending + postpaidPending;
    }


    public Long getSdRetailerCount(String sdId) {

        // SD -> Partner
        List<String> partnerIds =
                partnerRepository.findPartnerIdsBySd(sdId);
        if (partnerIds.isEmpty()) return 0L;

        // Partner -> Distributor
        List<String> distributorIds =
                distributorRepository.findDistributorIdsByPartners(partnerIds);
        if (distributorIds.isEmpty()) return 0L;

        // Distributor -> Retailer (COUNT)
        return retailerRepository.countRetailersByDistributors(distributorIds);
    }


    public List<Map<String, Object>> getSdLast4MonthsGraphData(String sdId) {

        // ===== LAST 4 MONTHS FROM DATE =====
        LocalDate fromDate = LocalDate.now()
                .minusMonths(3)
                .withDayOfMonth(1);

        // SD -> Partner
        List<String> partnerIds =
                partnerRepository.findPartnerIdsBySd(sdId);
        if (partnerIds.isEmpty()) return List.of();

        // Partner -> Distributor
        List<String> distributorIds =
                distributorRepository.findDistributorIdsByPartners(partnerIds);
        if (distributorIds.isEmpty()) return List.of();

        // Distributor -> Retailer
        List<String> retailerIds =
                retailerRepository.findRetailerIdsByDistributors(distributorIds);
        if (retailerIds.isEmpty()) return List.of();

        // ===== PREPAID =====
        List<MonthlyTransactionVolumeProjection> prepaidData =
                retailerPrepaidTransactionDetailsRepository
                        .getLast4MonthsTransactionVolumeForRetailers(
                                retailerIds, fromDate
                        );

        // ===== POSTPAID =====
        List<MonthlyTransactionVolumeProjection> postpaidData =
                retailerPostpaidTransactionDetailsRepository
                        .getLast4MonthsTransactionVolumeForRetailers(
                                retailerIds, fromDate
                        );

        // ===== MERGE MONTH WISE =====
        Map<String, BigDecimal> monthVsAmount = new HashMap<>();

        for (MonthlyTransactionVolumeProjection p : prepaidData) {
            monthVsAmount.merge(p.getMonth(), p.getValue(), BigDecimal::add);
        }

        for (MonthlyTransactionVolumeProjection p : postpaidData) {
            monthVsAmount.merge(p.getMonth(), p.getValue(), BigDecimal::add);
        }

        // ===== BUILD GRAPH DATA (ALWAYS 4 MONTHS) =====
        List<Map<String, Object>> graphData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 3; i >= 0; i--) {
            LocalDate monthDate = LocalDate.now().minusMonths(i);
            String monthKey = monthDate.format(formatter);

            graphData.add(
                    Map.of(
                            "month", monthKey,
                            "value", monthVsAmount.getOrDefault(monthKey, BigDecimal.ZERO)
                    )
            );
        }

        return graphData;
    }

    public BigDecimal getFundTransferred(String hierarchyId,
                                         LocalDate fromDate,
                                         LocalDate toDate) {

        LocalDateTime fromDateTime = null;
        LocalDateTime toDateTime = null;

        if (fromDate != null) {
            fromDateTime = fromDate.atStartOfDay(); // 00:00:00
        }

        if (toDate != null) {
            toDateTime = toDate.atTime(23, 59, 59); // end of day
        }

        return fundTransferRepository.sumFundTransferred(
                hierarchyId,
                fromDateTime,
                toDateTime
        );
    }

    public BigDecimal getFundReceived(String hierarchyId, LocalDate fromDate, LocalDate toDate) {
        return fundRequestRepository.sumFundReceived(hierarchyId, fromDate, toDate);
    }

    public BigDecimal getCommissionEarned(
            String hierarchyId,
            String hierarchyType,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        BigDecimal prepaid = BigDecimal.ZERO;
        BigDecimal postpaid = BigDecimal.ZERO;

        switch (hierarchyType) {

            case "SD" -> {
                prepaid = sdPrepaidTransactionDetailsRepository
                        .sumCommission(hierarchyId, fromDate, toDate);

                postpaid = sdPostpaidTransactionDetailsRepository
                        .sumCommission(hierarchyId, fromDate, toDate);
            }

            case "PT" -> {
                prepaid = ptPrepaidTransactionDetailsRepository
                        .sumCommission(hierarchyId, fromDate, toDate);

                postpaid = ptPostpaidTransactionDetailsRepository
                        .sumCommission(hierarchyId, fromDate, toDate);
            }

            case "DT" -> {
                prepaid = dtPrepaidTransactionDetailsRepository
                        .sumCommission(hierarchyId, fromDate, toDate);

                postpaid = dtPostpaidTransactionDetailsRepository
                        .sumCommission(hierarchyId, fromDate, toDate);
            }

            case "RT" -> {
                prepaid = retailerPrepaidTransactionDetailsRepository
                        .sumCommissionEarned(hierarchyId, fromDate, toDate);

                postpaid = retailerPostpaidTransactionDetailsRepository
                        .sumCommissionEarned(hierarchyId, fromDate, toDate);
            }
        }

        return prepaid.add(postpaid);
    }

    public HierarchyCountResponse getHierarchyCounts(String hierarchyId) {

        List<UserDetails> allChildren = new ArrayList<>();

        fetchChildrenRecursively(hierarchyId, allChildren);

        // Dynamic grouping by hierarchyCode
        Map<String, Long> countMap = allChildren.stream()
                .collect(Collectors.groupingBy(
                        UserDetails::getHierarchyCode,
                        Collectors.counting()
                ));

        long totalCount = countMap.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        HierarchyCountResponse response = new HierarchyCountResponse();
        response.setHierarchyId(hierarchyId);
        response.setCounts(countMap);
        response.setTotalCount(totalCount);

        return response;
    }


    public List<Map<String, Object>> getAdBarGraphData(
            String adId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        //  Get all retailers under AD
        List<String> retailerIds = getRetailersUnderHierarchy(adId);

        if (retailerIds.isEmpty()) return List.of();

        //  Fetch prepaid + postpaid
        List<ProductVolumeProjection> prepaid =
                retailerPrepaidTransactionDetailsRepository
                        .getProductWiseVolumeForRetailers(
                                retailerIds, fromDate, toDate);

        List<ProductVolumeProjection> postpaid =
                retailerPostpaidTransactionDetailsRepository
                        .getProductWiseVolumeForRetailers(
                                retailerIds, fromDate, toDate);

        //  Merge product wise
        Map<String, BigDecimal> productVsAmount = new HashMap<>();

        for (ProductVolumeProjection p : prepaid) {
            productVsAmount.merge(
                    p.getProductName(),
                    p.getTotalAmount(),
                    BigDecimal::add
            );
        }

        for (ProductVolumeProjection p : postpaid) {
            productVsAmount.merge(
                    p.getProductName(),
                    p.getTotalAmount(),
                    BigDecimal::add
            );
        }

        // 🔥 Convert to required format
        List<Map<String, Object>> barGraphData = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : productVsAmount.entrySet()) {
            barGraphData.add(
                    Map.of(
                            "product", entry.getKey(),
                            "volume", entry.getValue()
                    )
            );
        }

        return barGraphData;
    }


    private void fetchChildrenRecursively(
            String parentId,
            List<UserDetails> result
    ) {

        List<UserDetails> children =
                userDetailsRepository.findByParentAccount(parentId);

        for (UserDetails child : children) {
            result.add(child);
            fetchChildrenRecursively(child.getHierarchyId(), result);
        }
    }

    public BigDecimal getAdTransactionVolume(
            String adId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        //  Get all retailers under AD (including itself if RT)
        List<String> retailerIds = getRetailersUnderHierarchy(adId);

        if (retailerIds.isEmpty()) return BigDecimal.ZERO;

        BigDecimal prepaid =
                retailerPrepaidTransactionDetailsRepository
                        .sumTransactionVolumeForRetailers(
                                retailerIds,
                                fromDate,
                                toDate
                        );

        BigDecimal postpaid =
                retailerPostpaidTransactionDetailsRepository
                        .sumTransactionVolumeForRetailers(
                                retailerIds,
                                fromDate,
                                toDate
                        );

        return prepaid.add(postpaid);
    }

    public List<Map<String, Object>> getAdLineGraphData(
            String adId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        List<String> retailerIds = getRetailersUnderHierarchy(adId);

        if (retailerIds == null || retailerIds.isEmpty()) {
            return List.of();
        }

        List<ProductCountProjection> prepaid =
                retailerPrepaidTransactionDetailsRepository
                        .getProductWiseTransactionCount(retailerIds, fromDate, toDate);

        List<ProductCountProjection> postpaid =
                retailerPostpaidTransactionDetailsRepository
                        .getProductWiseTransactionCount(retailerIds, fromDate, toDate);

        Map<String, Long> productVsCount = new HashMap<>();

        if (prepaid != null) {
            for (ProductCountProjection p : prepaid) {
                productVsCount.merge(
                        p.getProductName(),
                        p.getTransactionCount() != null ? p.getTransactionCount() : 0L,
                        Long::sum
                );
            }
        }

        if (postpaid != null) {
            for (ProductCountProjection p : postpaid) {
                productVsCount.merge(
                        p.getProductName(),
                        p.getTransactionCount() != null ? p.getTransactionCount() : 0L,
                        Long::sum
                );
            }
        }

        return productVsCount.entrySet()
                .stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("product", entry.getKey());
                    map.put("transaction", entry.getValue());
                    return map;
                })
                .toList();
    }

    public Long getAdPendingTransactionCount(
            String adId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        List<String> retailerIds = getRetailersUnderHierarchy(adId);

        if (retailerIds == null || retailerIds.isEmpty()) {
            return 0L;
        }

        Long prepaidCount =
                retailerPrepaidTransactionDetailsRepository
                        .getPendingTransactionCount(retailerIds, fromDate, toDate);

        Long postpaidCount =
                retailerPostpaidTransactionDetailsRepository
                        .getPendingTransactionCount(retailerIds, fromDate, toDate);

        return (prepaidCount != null ? prepaidCount : 0L)
                + (postpaidCount != null ? postpaidCount : 0L);
    }

    public Map<String, Long> getOnboardingCounts() {

        long approved =
                retailerRepository.countByStatus("APPROVED") +
                        distributorRepository.countByStatus("APPROVED") +
                        partnerRepository.countByStatus("APPROVED") +
                        superDistributorRepository.countByStatus("APPROVED") +
                        whiteLabelRepository.countByStatus("APPROVED") +
                        enterprisesRepository.countByStatus("APPROVED") ;
//                        adminRepository.countByStatus("APPROVED");

        long rejected =
                retailerRepository.countByStatus("REJECTED") +
                        distributorRepository.countByStatus("REJECTED") +
                        partnerRepository.countByStatus("REJECTED") +
                        superDistributorRepository.countByStatus("REJECTED") +
                        whiteLabelRepository.countByStatus("REJECTED") +
                        enterprisesRepository.countByStatus("REJECTED") ;
//                        adminRepository.countByStatus("REJECTED");

        long pending =
                retailerRepository.countByStatus("PENDING") +
                        distributorRepository.countByStatus("PENDING") +
                        partnerRepository.countByStatus("PENDING") +
                        superDistributorRepository.countByStatus("PENDING") +
                        whiteLabelRepository.countByStatus("PENDING") +
                        enterprisesRepository.countByStatus("PENDING") ;
//                        adminRepository.countByStatus("PENDING");

        return Map.of(
                "APPROVED", approved,
                "REJECTED", rejected,
                "PENDING", pending
        );
    }

    public Map<String, BigDecimal> getAdTotalBalances(String hierarchyId) {

        List<String> allIds = getAllUsersUnderHierarchy(hierarchyId);

        if (allIds == null || allIds.isEmpty()) {
            return Map.of(
                    "PREPAID", BigDecimal.ZERO,
                    "POSTPAID", BigDecimal.ZERO
            );
        }

        BigDecimal totalPrepaid = BigDecimal.ZERO;
        BigDecimal totalPostpaid = BigDecimal.ZERO;

        // 🔹 Retailer
        totalPrepaid = totalPrepaid.add(
                retailerPrepaidAmountRepository
                        .sumAvailableBalanceByRetailers(allIds)
        );

        totalPostpaid = totalPostpaid.add(
                retailerPostpaidAmountRepository
                        .sumAvailableBalanceByRetailers(allIds)
        );

        // 🔹 Distributor
        totalPrepaid = totalPrepaid.add(
                distributorPrepaidAmountRepository
                        .sumAvailableBalanceByDistributors(allIds)
        );

        totalPostpaid = totalPostpaid.add(
                distributorPostpaidAmountRepository
                        .sumAvailableBalanceByDistributors(allIds)
        );

        // 🔹 Partner
        totalPrepaid = totalPrepaid.add(
                partnerPrepaidAmountRepository
                        .sumAvailableBalanceByPartners(allIds)
        );

        totalPostpaid = totalPostpaid.add(
                partnerPostpaidAmountRepository
                        .sumAvailableBalanceByPartners(allIds)
        );

        // 🔹 Enterprises
        totalPrepaid = totalPrepaid.add(
                enterprisesPrepaidAmountRepository
                        .sumAvailableBalanceByEnterprises(allIds)
        );

        totalPostpaid = totalPostpaid.add(
                enterprisesPostpaidAmountRepository
                        .sumAvailableBalanceByEnterprises(allIds)
        );

        // 🔹 Admin
        totalPrepaid = totalPrepaid.add(
                adminPrepaidAmountRepository
                        .sumAvailableBalanceByAdmins(allIds)
        );

        totalPostpaid = totalPostpaid.add(
                adminPostpaidAmountRepository
                        .sumAvailableBalanceByAdmins(allIds)
        );

        return Map.of(
                "PREPAID", totalPrepaid,
                "POSTPAID", totalPostpaid
        );
    }

    public List<String> getRetailersUnderHierarchy(String hierarchyId) {

        String type = commonMethods.extractHierarchyType(hierarchyId);

//        if ("RT".equals(type) || "RU".equals(type)) {
//            return List.of(hierarchyId);
//        }
        if ("RU".equals(type)) {
            return List.of(hierarchyId);
        }

        List<String> resultIds = new ArrayList<>();

        // Add itself
        resultIds.add(hierarchyId);

        List<UserDetails> allChildren = new ArrayList<>();

        fetchChildrenRecursively(hierarchyId, allChildren);

        log.info("All children IDs fetched: {}",
                allChildren.stream()
                        .map(UserDetails::getHierarchyId)
                        .toList());


        resultIds.addAll(
                allChildren.stream()
                        .filter(user ->
                                        "RT".equalsIgnoreCase(user.getHierarchyCode())
//                                "RU".equalsIgnoreCase(user.getHierarchyCode())
                        )
                        .map(UserDetails::getHierarchyId)
                        .toList()
        );

        return resultIds;
    }

    public List<String> getAllUsersUnderHierarchy(String hierarchyId) {

        String type = commonMethods.extractHierarchyType(hierarchyId);

//        if ("RT".equals(type) || "RU".equals(type)) {
//            return List.of(hierarchyId);
//        }
        if ("RU".equals(type)) {
            return List.of(hierarchyId);
        }

        List<String> resultIds = new ArrayList<>();

        // Add itself
        resultIds.add(hierarchyId);

        List<UserDetails> allChildren = new ArrayList<>();

        fetchChildrenRecursively(hierarchyId, allChildren);

        log.info("All children IDs fetched: {}",
                allChildren.stream()
                        .map(UserDetails::getHierarchyId)
                        .toList());


        resultIds.addAll(
                allChildren.stream()
                        .map(UserDetails::getHierarchyId)
                        .toList()
        );

        return resultIds;
    }

    public List<Map<String, Object>> getSdTop5Partners(
            String sdId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        List<String> partnerIds =
                partnerRepository.findPartnerIdsBySd(sdId);

        if (partnerIds.isEmpty()) {
            return List.of();
        }

        Map<String, BigDecimal> partnerBusiness = new HashMap<>();

        for (String partnerId : partnerIds) {

            // Partner -> Distributor
            List<String> distributorIds =
                    distributorRepository.findDistributorIdsByPartners(
                            List.of(partnerId)
                    );

            if (distributorIds.isEmpty()) {
                continue;
            }

            // Distributor -> Retailer
            List<String> retailerIds =
                    retailerRepository.findRetailerIdsByDistributors(
                            distributorIds
                    );

            if (retailerIds.isEmpty()) {
                continue;
            }

            BigDecimal prepaid =
                    retailerPrepaidTransactionDetailsRepository
                            .sumTransactionVolumeForRetailers(
                                    retailerIds, fromDate, toDate);

            BigDecimal postpaid =
                    retailerPostpaidTransactionDetailsRepository
                            .sumTransactionVolumeForRetailers(
                                    retailerIds, fromDate, toDate);

            // Safety if repository returns null
            if (prepaid == null) prepaid = BigDecimal.ZERO;
            if (postpaid == null) postpaid = BigDecimal.ZERO;

            BigDecimal total = prepaid.add(postpaid);

            partnerBusiness.put(partnerId, total);
        }

        return partnerBusiness.entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getKey());
                    map.put("totalBusiness", "₹ " + e.getValue());
                    return map;
                })
                .toList();
    }

    public BigDecimal getTransactionVolume(
            String hierarchyId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        List<String> retailerIds =
                getRetailersUnderHierarchy(hierarchyId);

        if (retailerIds.isEmpty()) return BigDecimal.ZERO;

        BigDecimal prepaid =
                retailerPrepaidTransactionDetailsRepository
                        .sumTransactionVolumeForRetailers(
                                retailerIds, fromDate, toDate);

        BigDecimal postpaid =
                retailerPostpaidTransactionDetailsRepository
                        .sumTransactionVolumeForRetailers(
                                retailerIds, fromDate, toDate);

        if (prepaid == null) prepaid = BigDecimal.ZERO;
        if (postpaid == null) postpaid = BigDecimal.ZERO;

        return prepaid.add(postpaid);
    }

    public Long getPendingTransactionCount(
            String hierarchyId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        List<String> retailerIds =
                getRetailersUnderHierarchy(hierarchyId);

        if (retailerIds.isEmpty()) return 0L;

        Long prepaid =
                retailerPrepaidTransactionDetailsRepository
                        .countPendingTransactionsForRetailers(
                                retailerIds, fromDate, toDate);

        Long postpaid =
                retailerPostpaidTransactionDetailsRepository
                        .countPendingTransactionsForRetailers(
                                retailerIds, fromDate, toDate);

        return prepaid + postpaid;
    }


    public List<Map<String, Object>> getLast4MonthsGraphData(String hierarchyId) {

        // ===== LAST 4 MONTHS START DATE =====
        LocalDate fromDate = LocalDate.now()
                .minusMonths(3)
                .withDayOfMonth(1);

        //  Recursive retailer resolution
        List<String> retailerIds = getRetailersUnderHierarchy(hierarchyId);

        if (retailerIds.isEmpty()) return List.of();

        // ===== PREPAID =====
        List<MonthlyTransactionVolumeProjection> prepaidData =
                retailerPrepaidTransactionDetailsRepository
                        .getLast4MonthsTransactionVolumeForRetailers(
                                retailerIds, fromDate
                        );

        // ===== POSTPAID =====
        List<MonthlyTransactionVolumeProjection> postpaidData =
                retailerPostpaidTransactionDetailsRepository
                        .getLast4MonthsTransactionVolumeForRetailers(
                                retailerIds, fromDate
                        );

        // ===== MERGE MONTH-WISE =====
        Map<String, BigDecimal> monthVsAmount = new HashMap<>();

        for (MonthlyTransactionVolumeProjection p : prepaidData) {
            monthVsAmount.merge(p.getMonth(), p.getValue(), BigDecimal::add);
        }

        for (MonthlyTransactionVolumeProjection p : postpaidData) {
            monthVsAmount.merge(p.getMonth(), p.getValue(), BigDecimal::add);
        }

        // ===== BUILD GRAPH DATA (ALWAYS 4 MONTHS) =====
        List<Map<String, Object>> graphData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 3; i >= 0; i--) {
            LocalDate monthDate = LocalDate.now().minusMonths(i);
            String monthKey = monthDate.format(formatter);

            graphData.add(
                    Map.of(
                            "month", monthKey,
                            "value", monthVsAmount.getOrDefault(monthKey, BigDecimal.ZERO)
                    )
            );
        }

        return graphData;
    }

    public List<Map<String, Object>> getTop5ByHierarchy(
            String hierarchyId,
            String type,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        //  Get direct children based on type
        List<String> childIds = switch (type) {

            case "WL" -> superDistributorRepository
                    .findSuperDistributorIdsByWhiteLabel(hierarchyId);

            case "EN" -> whiteLabelRepository
                    .findWhiteLabelIdsByEnterprise(hierarchyId);

            case "SD" -> partnerRepository
                    .findPartnerIdsBySd(hierarchyId);

            case "PT" -> distributorRepository
                    .findDistributorIdsByPartners(List.of(hierarchyId));

            case "DT" -> retailerRepository
                    .findRetailerIdsByDistributors(List.of(hierarchyId));

            default -> List.of();
        };

        if (childIds.isEmpty()) return List.of();

        Map<String, BigDecimal> childBusiness = new HashMap<>();

        for (String childId : childIds) {

            // Get all retailers under this child (recursive)
            List<String> retailerIds = getRetailersUnderHierarchy(childId);

            if (retailerIds.isEmpty()) continue;

            BigDecimal prepaid =
                    retailerPrepaidTransactionDetailsRepository
                            .sumTransactionVolumeForRetailers(
                                    retailerIds, fromDate, toDate);

            BigDecimal postpaid =
                    retailerPostpaidTransactionDetailsRepository
                            .sumTransactionVolumeForRetailers(
                                    retailerIds, fromDate, toDate);

            if (prepaid == null) prepaid = BigDecimal.ZERO;
            if (postpaid == null) postpaid = BigDecimal.ZERO;

            childBusiness.put(childId, prepaid.add(postpaid));
        }

        // 3⃣ Sort and return Top 5
        return childBusiness.entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getKey());
                    map.put("totalBusiness", "₹ " + e.getValue());
                    return map;
                })
                .toList();
    }

}




