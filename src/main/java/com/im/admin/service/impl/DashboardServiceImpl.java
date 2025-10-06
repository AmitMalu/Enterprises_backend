package com.im.admin.service.impl;


import com.im.admin.dto.DashboardResponseDto;
import com.im.admin.dto.WalletBalanceResponseDto;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.response.*;
import com.im.admin.service.DashboardService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

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
        MyBanksDto myBanksDto = new MyBanksDto();
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
                        banksDto.setBankName(bank.getBankName());
                        banksDto.setAccountName(bank.getBankHolderName() != null ? bank.getBankHolderName() : r.getName());
                        banksDto.setAccountNo(bank.getBankAccountNumber());
                        banksDto.setIFCSCode(bank.getIfscCode());
                        return banksDto;
                    })
                    .collect(Collectors.toList());

            response.setMyBanksList(myBanksDtoList);

            myBanksDto.setBankName(r.getBankDetails().getBankName());
            myBanksDto.setAccountName(r.getName());
            myBanksDto.setAccountNo(r.getBankDetails().getBankAccountNumber());
            myBanksDto.setIFCSCode(r.getBankDetails().getIfscCode());

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
                        banksDto.setBankName(bank.getBankName());
                        banksDto.setAccountName(bank.getBankHolderName());
                        banksDto.setAccountNo(bank.getBankAccountNumber());
                        banksDto.setIFCSCode(bank.getIfscCode());
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
                        banksDto.setBankName(bank.getBankName());
                        banksDto.setAccountName(bank.getBankHolderName());
                        banksDto.setAccountNo(bank.getBankAccountNumber());
                        banksDto.setIFCSCode(bank.getIfscCode());
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
                        banksDto.setBankName(bank.getBankName());
                        banksDto.setAccountName(bank.getBankHolderName());
                        banksDto.setAccountNo(bank.getBankAccountNumber());
                        banksDto.setIFCSCode(bank.getIfscCode());
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
                        banksDto.setBankName(bank.getBankName());
                        banksDto.setAccountName(bank.getBankHolderName());
                        banksDto.setAccountNo(bank.getBankAccountNumber());
                        banksDto.setIFCSCode(bank.getIfscCode());
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
                        banksDto.setBankName(bank.getBankName());
                        banksDto.setAccountName(bank.getBankHolderName());
                        banksDto.setAccountNo(bank.getBankAccountNumber());
                        banksDto.setIFCSCode(bank.getIfscCode());
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

}
