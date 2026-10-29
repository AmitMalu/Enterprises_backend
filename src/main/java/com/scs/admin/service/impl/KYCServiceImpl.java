package com.scs.admin.service.impl;

import com.scs.admin.dto.KYCEditRequest;
import com.scs.admin.dto.KYCRequestDto;
import com.scs.admin.entities.*;
import com.scs.admin.properties.FileUrlUtil;
import com.scs.admin.repositories.*;
import com.scs.admin.response.KYCApprovedResponseDto;
import com.scs.admin.response.KYCResponseDto;
import com.scs.admin.response.KycDocumentDto;
import com.scs.admin.service.HierarchyAccessService;
import com.scs.admin.service.KYCService;
import com.scs.admin.util.CommonMethods;
import com.scs.admin.util.ImageUtil;
import com.scs.admin.util.TokenUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;




import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class KYCServiceImpl implements KYCService {

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private WhiteLabelRepository whiteLabelsRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private ImageUtil imageUtil;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private HierarchyAccessService hierarchyAccessService;

    @Autowired
    private AccessHierarchyPathRepository accessHierarchyPathRepository;

    @Autowired
    private FileUrlUtil fileUrlUtil;


    @Override
    public KYCResponseDto searchByMobileNoAndHierarchyId(
            KYCRequestDto kycRequestDto
    ) {

        String loginHierarchyId =
                tokenUtil.extractHierarchyId();

        String searchKeyword =
                kycRequestDto.getSearchKeyword();

        KYCResponseDto result;

        String type =
                commonMethods.extractHierarchyType(
                        searchKeyword
                );

        /*
         * Search by hierarchyId
         */
        if (type != null
                && !"UNKNOWN"
                .equalsIgnoreCase(type)) {

            result =
                    getByHierarchyId(
                            type,
                            searchKeyword
                    );

        }

        /*
         * Search by mobile
         */
        else {

            result =
                    getByMobileNumber(
                            searchKeyword
                    );
        }

        if (result == null) {
            return null;
        }

        /*
         * ACCESS CHECK
         */
        if (!hierarchyAccessService.hasAccessToHierarchy(
                loginHierarchyId,
                result.getHierarchyId()
        )) {

            log.warn(
                    "Access denied. login={} target={}",
                    loginHierarchyId,
                    result.getHierarchyId()
            );

            return null;
        }

        return result;
    }

    private KYCResponseDto getByHierarchyId(String type, String hierarchyId) {
        return switch (type) {
            case "RT" -> retailerRepository.findById(hierarchyId)
                    .map(this::mapRetailer).orElse(null);
            case "DT" -> distributorRepository.findById(hierarchyId)
                    .map(this::mapDistributor).orElse(null);
            case "PT" -> partnerRepository.findById(hierarchyId)
                    .map(this::mapPartner).orElse(null);
            case "SD" -> superDistributorRepository.findById(hierarchyId)
                    .map(this::mapSuperDistributor).orElse(null);
            case "WL" -> whiteLabelsRepository.findById(hierarchyId)
                    .map(this::mapWhiteLabel).orElse(null);
            case "ET" -> enterprisesRepository.findById(hierarchyId)
                    .map(this::mapEnterprise).orElse(null);
            default -> null;
        };
    }

    private KYCResponseDto getByMobileNumber(String mobileNumber) {
        Retailer r = retailerRepository.findByMobileNumber(mobileNumber);
        if (r != null) return mapRetailer(r);

        Distributor d = distributorRepository.findByMobileNumber(mobileNumber);
        if (d != null) return mapDistributor(d);

        Partner p = partnerRepository.findByMobileNumber(mobileNumber);
        if (p != null) return mapPartner(p);

        SuperDistributor sd = superDistributorRepository.findByMobileNumber(mobileNumber);
        if (sd != null) return mapSuperDistributor(sd);

        WhiteLabels wl = whiteLabelsRepository.findByMobileNumber(mobileNumber);
        if (wl != null) return mapWhiteLabel(wl);

        Enterprises et = enterprisesRepository.findByMobileNumber(mobileNumber);
        if (et != null) return mapEnterprise(et);

        return null;
    }

    // ---- Mapping Methods ----
    private KYCResponseDto mapRetailer(Retailer r) {
        return new KYCResponseDto(
                r.getRetailerId(), r.getName(), r.getMobileNumber(),
                r.getEmailId()
        );
    }

    private KYCResponseDto mapDistributor(Distributor d) {
        return new KYCResponseDto(
                d.getDistributorId(), d.getName(), d.getMobileNumber(),
                d.getEmailId()
        );
    }

    private KYCResponseDto mapPartner(Partner p) {
        return new KYCResponseDto(
                p.getPartnerId(), p.getName(), p.getMobileNumber(),
                p.getEmailId()
        );
    }

    private KYCResponseDto mapSuperDistributor(SuperDistributor sd) {
        return new KYCResponseDto(
                sd.getSuperDistributorId(), sd.getName(), sd.getMobileNumber(),
                 sd.getEmailId()
        );
    }

    private KYCResponseDto mapWhiteLabel(WhiteLabels wl) {
        return new KYCResponseDto(
                wl.getWhiteLabelId(), wl.getTitle(), wl.getMobileNumber(),
                 wl.getEmailId()
        );
    }

    private KYCResponseDto mapEnterprise(Enterprises et) {
        return new KYCResponseDto(
                et.getEnterpriseId(), et.getName(), et.getMobileNumber(),
                et.getEmailId()
        );
    }

    @Override
    public Page<KYCResponseDto> searchByHierarchyWise(
            String hierarchyType,
            Pageable pageable
    ) {

        String loginHierarchyId =
                tokenUtil.extractHierarchyId();

        String loginType =
                commonMethods.extractHierarchyType(
                        loginHierarchyId
                );

        /*
         * ADMIN
         */
        if ("AD".equalsIgnoreCase(
                loginType
        )) {

            return switch (
                    hierarchyType.toUpperCase()
                    ) {

                case "RT" ->
                        retailerRepository
                                .findAll(pageable)
                                .map(this::mapRetailer);

                case "DT" ->
                        distributorRepository
                                .findAll(pageable)
                                .map(this::mapDistributor);

                case "PT" ->
                        partnerRepository
                                .findAll(pageable)
                                .map(this::mapPartner);

                case "SD" ->
                        superDistributorRepository
                                .findAll(pageable)
                                .map(this::mapSuperDistributor);

                case "WL" ->
                        whiteLabelsRepository
                                .findAll(pageable)
                                .map(this::mapWhiteLabel);

                case "ET" ->
                        enterprisesRepository
                                .findAll(pageable)
                                .map(this::mapEnterprise);

                default ->
                        Page.empty(
                                pageable
                        );
            };
        }

        /*
         * WL / subtree
         */

        List<String> allowedIds =
                accessHierarchyPathRepository
                        .findDescendantsByAncestor(
                                loginHierarchyId
                        );

        return switch (
                hierarchyType.toUpperCase()
                ) {

            case "RT" ->
                    retailerRepository
                            .findByRetailerIdIn(
                                    allowedIds,
                                    pageable
                            )
                            .map(
                                    this::mapRetailer
                            );

            case "DT" ->
                    distributorRepository
                            .findByDistributorIdIn(
                                    allowedIds,
                                    pageable
                            )
                            .map(
                                    this::mapDistributor
                            );

            case "PT" ->
                    partnerRepository
                            .findByPartnerIdIn(
                                    allowedIds,
                                    pageable
                            )
                            .map(
                                    this::mapPartner
                            );

            case "SD" ->
                    superDistributorRepository
                            .findBySuperDistributorIdIn(
                                    allowedIds,
                                    pageable
                            )
                            .map(
                                    this::mapSuperDistributor
                            );

            case "WL" ->
                    whiteLabelsRepository
                            .findByWhiteLabelIdIn(
                                    allowedIds,
                                    pageable
                            )
                            .map(
                                    this::mapWhiteLabel
                            );

            case "ET" ->
                    enterprisesRepository
                            .findByEnterpriseIdIn(
                                    allowedIds,
                                    pageable
                            )
                            .map(
                                    this::mapEnterprise
                            );

            default ->
                    Page.empty(
                            pageable
                    );
        };
    }

    @Override
    public String editKyc(KYCEditRequest request, MultipartFile imageFile) {
        String imagePath = null;

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String folder = "KYC_" + request.getCategory().toUpperCase();
                imagePath = imageUtil.saveImage(imageFile, folder);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to save image: " + e.getMessage();
        }

        String hierarchyId = request.getHierarchyId();
        String type = commonMethods.extractHierarchyType(request.getHierarchyId());
        String category = request.getCategory();
        boolean verified = request.isVerified();
        String docNumber = request.getDocNumber();


        final String finalImagePath = imagePath;

        switch (type) {
            case "RT" -> retailerRepository.findById(hierarchyId).ifPresent(r -> {
                updateDocument(r, category, verified, finalImagePath,docNumber);
                retailerRepository.save(r);
            });
            case "DT" -> distributorRepository.findById(hierarchyId).ifPresent(d -> {
                updateDocument(d, category, verified, finalImagePath,docNumber);
                distributorRepository.save(d);
            });
            case "PT" -> partnerRepository.findById(hierarchyId).ifPresent(p -> {
                updateDocument(p, category, verified, finalImagePath,docNumber);
                partnerRepository.save(p);
            });
            case "SD" -> superDistributorRepository.findById(hierarchyId).ifPresent(sd -> {
                updateDocument(sd, category, verified, finalImagePath,docNumber);
                superDistributorRepository.save(sd);
            });
            case "WL" -> whiteLabelsRepository.findById(hierarchyId).ifPresent(wl -> {
                updateDocument(wl, category, verified, finalImagePath,docNumber);
                whiteLabelsRepository.save(wl);
            });
            case "ET" -> enterprisesRepository.findById(hierarchyId).ifPresent(et -> {
                updateDocument(et, category, verified, finalImagePath,docNumber);
                enterprisesRepository.save(et);
            });
            default -> {
                return "Invalid type";
            }
        }

        return "KYC document updated successfully";
    }

    private String saveImage(MultipartFile imageFile, String folder) throws IOException {
        String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
        Path uploadDir = Paths.get(System.getProperty("user.home"), folder)
                .toAbsolutePath().normalize();

        // Create directory if it does not exist
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path filePath = uploadDir.resolve(fileName);

        // Copy the file to the target location
        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }


    private void updateDocument(Object entity, String category, boolean verified, String image,String docNumber) {
        switch (category.toUpperCase()) {
            case "AADHAAR_FRONT" -> {
                if (entity instanceof Retailer r) {
                    if (image != null) r.setAadhaarCardFrontImage(image);
                    r.setIsAadhaarVerified(verified);
                    r.setAadhaarNumber(docNumber);
                } else if (entity instanceof Distributor d) {
                    if (image != null) d.setAadhaarCardFrontImage(image);
                    d.setIsAadhaarVerified(verified);
                    d.setAadhaarNumber(docNumber);
                } else if (entity instanceof Partner p) {
                    if (image != null) p.setAadhaarCardFrontImage(image);
                    p.setIsAadhaarVerified(verified);
                    p.setAadhaarNumber(docNumber);
                } else if (entity instanceof SuperDistributor sd) {
                    if (image != null) sd.setAadhaarCardFrontImage(image);
                    sd.setIsAadhaarVerified(verified);
                    sd.setAadhaarNumber(docNumber);
                } else if (entity instanceof WhiteLabels wl) {
                    if (image != null) wl.setAadhaarCardFrontImage(image);
                    wl.setIsAadhaarVerified(verified);
                    wl.setAadhaarNumber(docNumber);
                } else if (entity instanceof Enterprises et) {
                    if (image != null) et.setAadhaarCardFrontImage(image);
                    et.setIsAadhaarVerified(verified);
                    et.setAadhaarNumber(docNumber);
                }
            }

            case "AADHAAR_BACK" -> {
                if (entity instanceof Retailer r) {
                    if (image != null) r.setAadhaarCardBackImage(image);
                    r.setIsAadhaarVerified(verified);
                    r.setAadhaarNumber(docNumber);
                } else if (entity instanceof Distributor d) {
                    if (image != null) d.setAadhaarCardBackImage(image);
                    d.setIsAadhaarVerified(verified);
                    d.setAadhaarNumber(docNumber);
                } else if (entity instanceof Partner p) {
                    if (image != null) p.setAadhaarCardBackImage(image);
                    p.setIsAadhaarVerified(verified);
                    p.setAadhaarNumber(docNumber);
                } else if (entity instanceof SuperDistributor sd) {
                    if (image != null) sd.setAadhaarCardBackImage(image);
                    sd.setIsAadhaarVerified(verified);
                    sd.setAadhaarNumber(docNumber);
                } else if (entity instanceof WhiteLabels wl) {
                    if (image != null) wl.setAadhaarCardBackImage(image);
                    wl.setIsAadhaarVerified(verified);
                    wl.setAadhaarNumber(docNumber);
                } else if (entity instanceof Enterprises et) {
                    if (image != null) et.setAadhaarCardBackImage(image);
                    et.setIsAadhaarVerified(verified);
                    et.setAadhaarNumber(docNumber);
                }
            }

            case "PAN" -> {
                if (entity instanceof Retailer r) {
                    if (image != null) r.setPanCardImage(image);
                    r.setPanVerified(verified);
                    r.setPanNumber(docNumber);
                } else if (entity instanceof Distributor d) {
                    if (image != null) d.setPanCardImage(image);
                    d.setPanVerified(verified);
                    d.setPanNumber(docNumber);
                } else if (entity instanceof Partner p) {
                    if (image != null) p.setPanCardImage(image);
                    p.setPanVerified(verified);
                    p.setPanNumber(docNumber);
                } else if (entity instanceof SuperDistributor sd) {
                    if (image != null) sd.setPanCardImage(image);
                    sd.setPanVerified(verified);
                    sd.setPanNumber(docNumber);
                } else if (entity instanceof WhiteLabels wl) {
                    if (image != null) wl.setPanCardImage(image);
                    wl.setPanVerified(verified);
                    wl.setPanNumber(docNumber);
                } else if (entity instanceof Enterprises et) {
                    if (image != null) et.setPanCardImage(image);
                    et.setPanVerified(verified);
                    et.setPanNumber(docNumber);
                }
            }

            case "SHOP" -> {
                if (entity instanceof Retailer r) {
                    if (image != null) r.setShopImage(image);
                    r.setShopNumber(docNumber);
                } else if (entity instanceof Distributor d) {
                    if (image != null) d.setShopImage(image);
                    d.setShopNumber(docNumber);
                } else if (entity instanceof Partner p) {
                    if (image != null) p.setShopImage(image);
                    p.setShopNumber(docNumber);
                } else if (entity instanceof SuperDistributor sd) {
                    if (image != null) sd.setShopImage(image);
                    sd.setShopNumber(docNumber);
                } else if (entity instanceof WhiteLabels wl) {
                    if (image != null) wl.setShopImage(image);
                    wl.setShopNumber(docNumber);
                } else if (entity instanceof Enterprises et) {
                    if (image != null) et.setShopImage(image);
                    et.setShopNumber(docNumber);
                }
            }

            case "BUSINESS" -> {
                if (entity instanceof Retailer r) {
                    if (image != null) r.setBusinessProofImage(image);
                    r.setGstNumberVerified(verified);
                    r.setBusinessNumber(docNumber);
                } else if (entity instanceof Distributor d) {
                    if (image != null) d.setBusinessProofImage(image);
                    d.setGstNumberVerified(verified);
                    d.setBusinessNumber(docNumber);
                } else if (entity instanceof Partner p) {
                    if (image != null) p.setBusinessProofImage(image);
                    p.setGstNumberVerified(verified);
                    p.setBusinessNumber(docNumber);
                } else if (entity instanceof SuperDistributor sd) {
                    if (image != null) sd.setBusinessProofImage(image);
                    sd.setGstNumberVerified(verified);
                    sd.setBusinessNumber(docNumber);
                } else if (entity instanceof WhiteLabels wl) {
                    if (image != null) wl.setBusinessProofImage(image);
                    wl.setGstNumberVerified(verified);
                } else if (entity instanceof Enterprises et) {
                    if (image != null) et.setBusinessProofImage(image);
                    et.setGstNumberVerified(verified);
                    et.setBusinessNumber(docNumber);
                }
            }

            case "EMAIL" -> {
                if (entity instanceof Retailer r) {
                    r.setEmailVerified(verified);
                    r.setEmailId(docNumber);
                } else if (entity instanceof Distributor d) {
                    d.setEmailVerified(verified);
                } else if (entity instanceof Partner p) {
                    p.setEmailVerified(verified);
                    p.setEmailId(docNumber);
                } else if (entity instanceof SuperDistributor sd) {
                    sd.setEmailVerified(verified);
                    sd.setEmailId(docNumber);
                } else if (entity instanceof WhiteLabels wl) {
                    wl.setEmailVerified(verified);
                    wl.setEmailId(docNumber);
                } else if (entity instanceof Enterprises et) {
                    et.setEmailVerified(verified);
                    et.setEmailId(docNumber);
                }
            }

            case "AADHAAR_PAN_VERIFIED" -> {
                if (entity instanceof Retailer r) {
                    r.setAadhaarPanVerified(verified);
                    r.setAadhaarNumber(docNumber);
                } else if (entity instanceof Distributor d) {
                    d.setAadhaarPanVerified(verified);
                    d.setAadhaarNumber(docNumber);
                } else if (entity instanceof Partner p) {
                    p.setAadhaarPanVerified(verified);
                    p.setAadhaarNumber(docNumber);
                } else if (entity instanceof SuperDistributor sd) {
                    sd.setAadhaarPanVerified(verified);
                    sd.setAadhaarNumber(docNumber);
                } else if (entity instanceof WhiteLabels wl) {
                    wl.setAadhaarPanVerified(verified);
                    wl.setAadhaarNumber(docNumber);
                } else if (entity instanceof Enterprises et) {
                    et.setAadhaarPanVerified(verified);
                    et.setAadhaarNumber(docNumber);
                    et.setAadhaarNumber(docNumber);
                }
            }
        }
}


    @Transactional
    @Override
    public void deleteKycDocument(String hierarchyId, String category) {

        String type = commonMethods.extractHierarchyType(hierarchyId);

        switch (type) {
            case "RT" -> retailerRepository.findById(hierarchyId).ifPresent(r -> {
                removeDocumentField(r, category);
                retailerRepository.save(r);
            });
            case "DT" -> distributorRepository.findById(hierarchyId).ifPresent(d -> {
                removeDocumentField(d, category);
                distributorRepository.save(d);
            });

            case "PT" -> partnerRepository.findById(hierarchyId).ifPresent(p -> {
                removeDocumentField(p, category);
                partnerRepository.save(p);
            });
            case "SD" -> superDistributorRepository.findById(hierarchyId).ifPresent(sd -> {
                removeDocumentField(sd, category);
                superDistributorRepository.save(sd);
            });
            case "WL" -> whiteLabelsRepository.findById(hierarchyId).ifPresent(wl -> {
                removeDocumentField(wl, category);
                whiteLabelsRepository.save(wl);
            });
            case "ET" -> enterprisesRepository.findById(hierarchyId).ifPresent(et -> {
                removeDocumentField(et, category);
                enterprisesRepository.save(et);
            });
            default -> throw new IllegalArgumentException("Invalid type");
        }
    }


    private void removeDocumentField(Object entity, String category) {
        switch (category.toUpperCase()) {
            case "AADHAAR_FRONT" -> {
                if (entity instanceof Retailer r) {
                    r.setAadhaarCardFrontImage(null);
                } else if (entity instanceof Distributor d) {
                    d.setAadhaarCardFrontImage(null);
                } else if (entity instanceof Partner p) {
                    p.setAadhaarCardFrontImage(null);
                } else if (entity instanceof SuperDistributor sd) {
                    sd.setAadhaarCardFrontImage(null);
                } else if (entity instanceof WhiteLabels wl) {
                    wl.setAadhaarCardFrontImage(null);
                } else if (entity instanceof Enterprises et) {
                    et.setAadhaarCardFrontImage(null);
                }
            }

            case "AADHAAR_BACK" -> {
                if (entity instanceof Retailer r) {
                    r.setAadhaarCardBackImage(null);
                } else if (entity instanceof Distributor d) {
                    d.setAadhaarCardBackImage(null);
                } else if (entity instanceof Partner p) {
                    p.setAadhaarCardBackImage(null);
                } else if (entity instanceof SuperDistributor sd) {
                    sd.setAadhaarCardBackImage(null);
                } else if (entity instanceof WhiteLabels wl) {
                    wl.setAadhaarCardBackImage(null);
                } else if (entity instanceof Enterprises et) {
                    et.setAadhaarCardBackImage(null);
                }
            }

            case "PAN" -> {
                if (entity instanceof Retailer r) {
                    r.setPanCardImage(null);
                } else if (entity instanceof Distributor d) {
                    d.setPanCardImage(null);
                } else if (entity instanceof Partner p) {
                    p.setPanCardImage(null);
                } else if (entity instanceof SuperDistributor sd) {
                    sd.setPanCardImage(null);
                } else if (entity instanceof WhiteLabels wl) {
                    wl.setPanCardImage(null);
                } else if (entity instanceof Enterprises et) {
                    et.setPanCardImage(null);
                }
            }

            case "SHOP" -> {
                if (entity instanceof Retailer r) {
                    r.setShopImage(null);
                } else if (entity instanceof Distributor d) {
                    d.setShopImage(null);
                } else if (entity instanceof Partner p) {
                    p.setShopImage(null);
                } else if (entity instanceof SuperDistributor sd) {
                    sd.setShopImage(null);
                } else if (entity instanceof WhiteLabels wl) {
                    wl.setShopImage(null);
                } else if (entity instanceof Enterprises et) {
                    et.setShopImage(null);
                }
            }

            case "BUSINESS" -> {
                if (entity instanceof Retailer r) {
                    r.setBusinessProofImage(null);
                } else if (entity instanceof Distributor d) {
                    d.setBusinessProofImage(null);
                } else if (entity instanceof Partner p) {
                    p.setBusinessProofImage(null);
                } else if (entity instanceof SuperDistributor sd) {
                    sd.setBusinessProofImage(null);
                } else if (entity instanceof WhiteLabels wl) {
                    wl.setBusinessProofImage(null);
                } else if (entity instanceof Enterprises et) {
                    et.setBusinessProofImage(null);
                }
            }


        }
    }


    @Override
    public Page<Object> getApprovedDocuments(String searchKeyword, Pageable pageable) {

        switch (searchKeyword.toUpperCase()) {
            case "RT", "DT", "PT", "SD", "WL", "ET" -> {
                Page<?> pageData = switch (searchKeyword.toUpperCase()) {
                    case "RT" -> retailerRepository.findAll(pageable);
                    case "DT" -> distributorRepository.findAll(pageable);
                    case "PT" -> partnerRepository.findAll(pageable);
                    case "SD" -> superDistributorRepository.findAll(pageable);
                    case "WL" -> whiteLabelsRepository.findAll(pageable);
                    case "ET" -> enterprisesRepository.findAll(pageable);
                    default -> Page.empty(pageable);
                };

                List<Object> mappedList = pageData.stream()
                        .filter(this::hasApprovedDocuments)
                        .map(entity -> {
                            if (entity instanceof Retailer) return mapApprovedRetailer((Retailer) entity);
                            if (entity instanceof Distributor) return mapApprovedDistributor((Distributor) entity);
                            if (entity instanceof Partner) return mapApprovedPartner((Partner) entity);
                            if (entity instanceof SuperDistributor) return mapApprovedSuperDistributor((SuperDistributor) entity);
                            if (entity instanceof WhiteLabels) return mapApprovedWhiteLabel((WhiteLabels) entity);
                            if (entity instanceof Enterprises) return mapApprovedEnterprise((Enterprises) entity);
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                return new PageImpl<>(mappedList, pageable, pageData.getTotalElements());
            }

            default -> {
                String type = commonMethods.extractHierarchyType(searchKeyword);

                if (type != null && !"UNKNOWN".equalsIgnoreCase(type)) {
                    Object singleEntity = switch (type) {
                        case "RT" -> retailerRepository.findById(searchKeyword)
                                .filter(this::hasApprovedDocuments)
                                .map(this::mapApprovedRetailer)
                                .orElse(null);

                        case "DT" -> distributorRepository.findById(searchKeyword)
                                .filter(this::hasApprovedDocuments)
                                .map(this::mapApprovedDistributor)
                                .orElse(null);

                        case "PT" -> partnerRepository.findById(searchKeyword)
                                .filter(this::hasApprovedDocuments)
                                .map(this::mapApprovedPartner)
                                .orElse(null);

                        case "SD" -> superDistributorRepository.findById(searchKeyword)
                                .filter(this::hasApprovedDocuments)
                                .map(this::mapApprovedSuperDistributor)
                                .orElse(null);

                        case "WL" -> whiteLabelsRepository.findById(searchKeyword)
                                .filter(this::hasApprovedDocuments)
                                .map(this::mapApprovedWhiteLabel)
                                .orElse(null);

                        case "ET" -> enterprisesRepository.findById(searchKeyword)
                                .filter(this::hasApprovedDocuments)
                                .map(this::mapApprovedEnterprise)
                                .orElse(null);

                        default -> null;
                    };

                    List<Object> list = (singleEntity != null) ? List.of(singleEntity) : List.of();
                    return new PageImpl<>(list, pageable, list.size());
                } else {
                    List<Object> resultList = new ArrayList<>();

                    Retailer r = retailerRepository.findByMobileNumber(searchKeyword);
                    if (r != null && hasApprovedDocuments(r)) resultList.add(mapRetailer(r));

                    Distributor d = distributorRepository.findByMobileNumber(searchKeyword);
                    if (d != null && hasApprovedDocuments(d)) resultList.add(mapDistributor(d));

                    Partner p = partnerRepository.findByMobileNumber(searchKeyword);
                    if (p != null && hasApprovedDocuments(p)) resultList.add(mapPartner(p));

                    SuperDistributor sd = superDistributorRepository.findByMobileNumber(searchKeyword);
                    if (sd != null && hasApprovedDocuments(sd)) resultList.add(mapSuperDistributor(sd));

                    WhiteLabels wl = whiteLabelsRepository.findByMobileNumber(searchKeyword);
                    if (wl != null && hasApprovedDocuments(wl)) resultList.add(mapWhiteLabel(wl));

                    Enterprises et = enterprisesRepository.findByMobileNumber(searchKeyword);
                    if (et != null && hasApprovedDocuments(et)) resultList.add(mapEnterprise(et));

                    return new PageImpl<>(resultList, pageable, resultList.size());
                }
            }
        }
    }

    private boolean hasApprovedDocuments(Object entity) {
        if (entity instanceof Retailer r) {
            return Boolean.TRUE.equals(r.getEmailVerified())  // Mandatory
                    && (r.getAadhaarCardFrontImage() != null && Boolean.TRUE.equals(r.getIsAadhaarVerified()))
                    && (r.getAadhaarCardBackImage() != null && Boolean.TRUE.equals(r.getIsAadhaarVerified()))
                    && (r.getPanCardImage() != null && Boolean.TRUE.equals(r.getPanVerified()))
                    && (r.getShopImage() != null)
                    && (r.getBusinessProofImage() != null && Boolean.TRUE.equals(r.getGstNumberVerified()));
        } else if (entity instanceof Distributor d) {
            return Boolean.TRUE.equals(d.getEmailVerified())
                    && (d.getAadhaarCardFrontImage() != null && Boolean.TRUE.equals(d.getIsAadhaarVerified()))
                    && (d.getAadhaarCardBackImage() != null && Boolean.TRUE.equals(d.getIsAadhaarVerified()))
                    && (d.getPanCardImage() != null && Boolean.TRUE.equals(d.getPanVerified()))
                    && (d.getShopImage() != null)
                    && (d.getBusinessProofImage() != null && Boolean.TRUE.equals(d.getGstNumberVerified()));
        } else if (entity instanceof Partner p) {
            return Boolean.TRUE.equals(p.getEmailVerified())
                    && (p.getAadhaarCardFrontImage() != null && Boolean.TRUE.equals(p.getIsAadhaarVerified()))
                    && (p.getAadhaarCardBackImage() != null && Boolean.TRUE.equals(p.getIsAadhaarVerified()))
                    && (p.getPanCardImage() != null && Boolean.TRUE.equals(p.getPanVerified()))
                    && (p.getShopImage() != null)
                    && (p.getBusinessProofImage() != null && Boolean.TRUE.equals(p.getGstNumberVerified()));
        } else if (entity instanceof SuperDistributor sd) {
            return Boolean.TRUE.equals(sd.getEmailVerified())
                    && (sd.getAadhaarCardFrontImage() != null && Boolean.TRUE.equals(sd.getIsAadhaarVerified()))
                    && (sd.getAadhaarCardBackImage() != null && Boolean.TRUE.equals(sd.getIsAadhaarVerified()))
                    && (sd.getPanCardImage() != null && Boolean.TRUE.equals(sd.getPanVerified()))
                    && (sd.getShopImage() != null)
                    && (sd.getBusinessProofImage() != null && Boolean.TRUE.equals(sd.getGstNumberVerified()));
        } else if (entity instanceof WhiteLabels wl) {
            return Boolean.TRUE.equals(wl.getEmailVerified())
                    && (wl.getAadhaarCardFrontImage() != null && Boolean.TRUE.equals(wl.getIsAadhaarVerified()))
                    && (wl.getAadhaarCardBackImage() != null && Boolean.TRUE.equals(wl.getIsAadhaarVerified()))
                    && (wl.getPanCardImage() != null && Boolean.TRUE.equals(wl.getPanVerified()))
                    && (wl.getShopImage() != null)
                    && (wl.getBusinessProofImage() != null && Boolean.TRUE.equals(wl.getGstNumberVerified()));
        } else if (entity instanceof Enterprises et) {
            return Boolean.TRUE.equals(et.getEmailVerified())
                    && (et.getAadhaarCardFrontImage() != null && Boolean.TRUE.equals(et.getIsAadhaarVerified()))
                    && (et.getAadhaarCardBackImage() != null && Boolean.TRUE.equals(et.getIsAadhaarVerified()))
                    && (et.getPanCardImage() != null && Boolean.TRUE.equals(et.getPanVerified()))
                    && (et.getShopImage() != null)
                    && (et.getBusinessProofImage() != null && Boolean.TRUE.equals(et.getGstNumberVerified()));
        }

        return false;
    }


    // ---- Mapping Methods ----
    private KYCApprovedResponseDto mapApprovedRetailer(Retailer r) {
        return new KYCApprovedResponseDto(
                r.getRetailerId(), r.getName(), r.getMobileNumber(),
                r.getEmailId(), r.getEmailVerified(),
                r.getAadhaarCardFrontImage(), r.getAadhaarCardBackImage(), r.getIsAadhaarVerified(), r.getAadhaarNumber(),
                r.getPanCardImage(), r.getPanNumber(), r.getPanVerified(), r.getAadhaarPanVerified(),
                r.getShopImage(), r.getShopNumber(), r.getBusinessProofImage(), r.getBusinessNumber(), r.getGstNumberVerified()
        );
    }

    private KYCApprovedResponseDto mapApprovedDistributor(Distributor d) {
        return new KYCApprovedResponseDto(
                d.getDistributorId(), d.getName(), d.getMobileNumber(),
                d.getEmailId(), d.getEmailVerified(),
                d.getAadhaarCardFrontImage(), d.getAadhaarCardBackImage(), d.getIsAadhaarVerified(), d.getAadhaarNumber(),
                d.getPanCardImage(), d.getPanNumber(), d.getPanVerified(), d.getAadhaarPanVerified(),
                d.getShopImage(), d.getShopNumber(), d.getBusinessProofImage(), d.getBusinessNumber(), d.getGstNumberVerified()
        );
    }

    private KYCApprovedResponseDto mapApprovedPartner(Partner p) {
        return new KYCApprovedResponseDto(
                p.getPartnerId(), p.getName(), p.getMobileNumber(),
                p.getEmailId(), p.getEmailVerified(),
                p.getAadhaarCardFrontImage(), p.getAadhaarCardBackImage(), p.getIsAadhaarVerified(), p.getAadhaarNumber(),
                p.getPanCardImage(), p.getPanNumber(), p.getPanVerified(), p.getAadhaarPanVerified(),
                p.getShopImage(), p.getShopNumber(), p.getBusinessProofImage(), p.getBusinessNumber(), p.getGstNumberVerified()
        );
    }

    private KYCApprovedResponseDto mapApprovedSuperDistributor(SuperDistributor sd) {
        return new KYCApprovedResponseDto(
                sd.getSuperDistributorId(), sd.getName(), sd.getMobileNumber(),
                sd.getEmailId(), sd.getEmailVerified(),
                sd.getAadhaarCardFrontImage(), sd.getAadhaarCardBackImage(), sd.getIsAadhaarVerified(), sd.getAadhaarNumber(),
                sd.getPanCardImage(), sd.getPanNumber(), sd.getPanVerified(), sd.getAadhaarPanVerified(),
                sd.getShopImage(), sd.getShopNumber(), sd.getBusinessProofImage(), sd.getBusinessNumber(), sd.getGstNumberVerified()
        );
    }

    private KYCApprovedResponseDto mapApprovedWhiteLabel(WhiteLabels wl) {
        return new KYCApprovedResponseDto(
                wl.getWhiteLabelId(), wl.getTitle(), wl.getMobileNumber(),
                wl.getEmailId(), wl.getEmailVerified(),
                wl.getAadhaarCardFrontImage(), wl.getAadhaarCardBackImage(), wl.getIsAadhaarVerified(), wl.getAadhaarNumber(),
                wl.getPanCardImage(), wl.getPanNumber(), wl.getPanVerified(), wl.getAadhaarPanVerified(),
                wl.getShopImage(), wl.getShopNumber(), wl.getBusinessProofImage(), wl.getBusinessNumber(), wl.getGstNumberVerified()
        );
    }

    private KYCApprovedResponseDto mapApprovedEnterprise(Enterprises et) {
        return new KYCApprovedResponseDto(
                et.getEnterpriseId(), et.getName(), et.getMobileNumber(),
                et.getEmailId(), et.getEmailVerified(),
                et.getAadhaarCardFrontImage(), et.getAadhaarCardBackImage(), et.getIsAadhaarVerified(), et.getAadhaarNumber(),
                et.getPanCardImage(), et.getPanNumber(), et.getPanVerified(), et.getAadhaarPanVerified(),
                et.getShopImage(), et.getShopNumber(), et.getBusinessProofImage(), et.getBusinessNumber(), et.getGstNumberVerified()
        );
    }


    @Override
    public List<KycDocumentDto> getKycDetailDocumentsByHierarchyId(String hierarchyId) {
        String type = commonMethods.extractHierarchyType(hierarchyId);

        if (type == null || "UNKNOWN".equalsIgnoreCase(type)) {
            return Collections.emptyList();
        }

        Object entity = switch (type) {
            case "RT" -> retailerRepository.findById(hierarchyId).orElse(null);
            case "DT" -> distributorRepository.findById(hierarchyId).orElse(null);
            case "PT" -> partnerRepository.findById(hierarchyId).orElse(null);
            case "SD" -> superDistributorRepository.findById(hierarchyId).orElse(null);
            case "WL" -> whiteLabelsRepository.findById(hierarchyId).orElse(null);
            case "ET" -> enterprisesRepository.findById(hierarchyId).orElse(null);
            default -> null;
        };

        if (entity == null) return Collections.emptyList();

        return mapEntityToDocumentList(entity);
    }

    private List<KycDocumentDto> mapEntityToDocumentList(Object entity) {
        String email = null;
        String aadhaarNumber = null;
        String aadhaarFront = null, aadhaarBack = null;
        String panNumber = null, panImage = null;
        String shopNumber = null, shopImage = null;
        String businessNumber = null, businessProofImage = null;

        Boolean isEmailVerified = null;
        Boolean isAadhaarVerified = null;
        Boolean aadhaarPanVerified = null;
        Boolean gstNumberVerified = null;
        Boolean panVerified = null;

        if (entity instanceof Retailer r) {
            email = r.getEmailId();
            aadhaarNumber = r.getAadhaarNumber();
            aadhaarFront = fileUrlUtil.getFullUrl(r.getAadhaarCardFrontImage());
            aadhaarBack = fileUrlUtil.getFullUrl(r.getAadhaarCardBackImage());
            panNumber = r.getPanNumber();
            panImage = fileUrlUtil.getFullUrl(r.getPanCardImage());
            shopNumber = r.getShopNumber();
            shopImage = fileUrlUtil.getFullUrl(r.getShopImage());
            businessNumber = r.getBusinessNumber();
            businessProofImage = fileUrlUtil.getFullUrl(r.getBusinessProofImage());
            isEmailVerified = r.getEmailVerified();
            isAadhaarVerified = r.getIsAadhaarVerified();
            aadhaarPanVerified = r.getAadhaarPanVerified();
            gstNumberVerified = r.getGstNumberVerified();
            panVerified = r.getPanVerified();
        }
        else if (entity instanceof Distributor d) {
            email = d.getEmailId();
            aadhaarNumber = d.getAadhaarNumber();
            aadhaarFront = fileUrlUtil.getFullUrl(d.getAadhaarCardFrontImage());
            aadhaarBack = fileUrlUtil.getFullUrl(d.getAadhaarCardBackImage());
            panNumber = d.getPanNumber();
            panImage = fileUrlUtil.getFullUrl(d.getPanCardImage());
            shopNumber = d.getShopNumber();
            shopImage = fileUrlUtil.getFullUrl(d.getShopImage());
            businessNumber = d.getBusinessNumber();
            businessProofImage = fileUrlUtil.getFullUrl(d.getBusinessProofImage());
            isEmailVerified = d.getEmailVerified();
            isAadhaarVerified = d.getIsAadhaarVerified();
            aadhaarPanVerified = d.getAadhaarPanVerified();
            gstNumberVerified = d.getGstNumberVerified();
            panVerified = d.getPanVerified();
        }
        else if (entity instanceof Partner p) {
            email = p.getEmailId();
            aadhaarNumber = p.getAadhaarNumber();
            aadhaarFront = fileUrlUtil.getFullUrl(p.getAadhaarCardFrontImage());
            aadhaarBack = fileUrlUtil.getFullUrl(p.getAadhaarCardBackImage());
            panNumber = p.getPanNumber();
            panImage = fileUrlUtil.getFullUrl(p.getPanCardImage());
            shopNumber = p.getShopNumber();
            shopImage = fileUrlUtil.getFullUrl(p.getShopImage());
            businessNumber = p.getBusinessNumber();
            businessProofImage = fileUrlUtil.getFullUrl(p.getBusinessProofImage());
            isEmailVerified = p.getEmailVerified();
            isAadhaarVerified = p.getIsAadhaarVerified();
            aadhaarPanVerified = p.getAadhaarPanVerified();
            gstNumberVerified = p.getGstNumberVerified();
            panVerified = p.getPanVerified();
        }
        else if (entity instanceof SuperDistributor sd) {
            email = sd.getEmailId();
            aadhaarNumber = sd.getAadhaarNumber();
            aadhaarFront = fileUrlUtil.getFullUrl(sd.getAadhaarCardFrontImage());
            aadhaarBack = fileUrlUtil.getFullUrl(sd.getAadhaarCardBackImage());
            panNumber = sd.getPanNumber();
            panImage = fileUrlUtil.getFullUrl(sd.getPanCardImage());
            shopNumber = sd.getShopNumber();
            shopImage = fileUrlUtil.getFullUrl(sd.getShopImage());
            businessNumber = sd.getBusinessNumber();
            businessProofImage = fileUrlUtil.getFullUrl(sd.getBusinessProofImage());
            isEmailVerified = sd.getEmailVerified();
            isAadhaarVerified = sd.getIsAadhaarVerified();
            aadhaarPanVerified = sd.getAadhaarPanVerified();
            gstNumberVerified = sd.getGstNumberVerified();
            panVerified = sd.getPanVerified();
        }
        else if (entity instanceof WhiteLabels wl) {
            email = wl.getEmailId();
            aadhaarNumber = wl.getAadhaarNumber();
            aadhaarFront = fileUrlUtil.getFullUrl(wl.getAadhaarCardFrontImage());
            aadhaarBack = fileUrlUtil.getFullUrl(wl.getAadhaarCardBackImage());
            panNumber = wl.getPanNumber();
            panImage = fileUrlUtil.getFullUrl(wl.getPanCardImage());
            shopNumber = wl.getShopNumber();
            shopImage = fileUrlUtil.getFullUrl(wl.getShopImage());
            businessNumber = wl.getBusinessNumber();
            businessProofImage = fileUrlUtil.getFullUrl(wl.getBusinessProofImage());
            isEmailVerified = wl.getEmailVerified();
            isAadhaarVerified = wl.getIsAadhaarVerified();
            aadhaarPanVerified = wl.getAadhaarPanVerified();
            gstNumberVerified = wl.getGstNumberVerified();
            panVerified = wl.getPanVerified();
        }
        else if (entity instanceof Enterprises et) {
            email = et.getEmailId();
            aadhaarNumber = et.getAadhaarNumber();
            aadhaarFront = fileUrlUtil.getFullUrl(et.getAadhaarCardFrontImage());
            aadhaarBack = fileUrlUtil.getFullUrl(et.getAadhaarCardBackImage());
            panNumber = et.getPanNumber();
            panImage = fileUrlUtil.getFullUrl(et.getPanCardImage());
            shopNumber = et.getShopNumber();
            shopImage = fileUrlUtil.getFullUrl(et.getShopImage());
            businessNumber = et.getBusinessNumber();
            businessProofImage = fileUrlUtil.getFullUrl(et.getBusinessProofImage());
            isEmailVerified = et.getEmailVerified();
            isAadhaarVerified = et.getIsAadhaarVerified();
            aadhaarPanVerified = et.getAadhaarPanVerified();
            gstNumberVerified = et.getGstNumberVerified();
            panVerified = et.getPanVerified();
        }

        return List.of(
                new KycDocumentDto("EMAIL",
                        isEmailVerified == null ? "null" : isEmailVerified ? "true" : "false",
                        email, null),

                new KycDocumentDto("AADHAAR_FRONT",
                        isAadhaarVerified == null ? "null" : isAadhaarVerified ? "true" : "false",
                        aadhaarNumber, aadhaarFront),

                new KycDocumentDto("AADHAAR_BACK",
                        isAadhaarVerified == null ? "null" : isAadhaarVerified ? "true" : "false",
                        aadhaarNumber, aadhaarBack),

                new KycDocumentDto("PAN",
                        panVerified == null ? "null" : panVerified ? "true" : "false",
                        panNumber, panImage),

                new KycDocumentDto("SHOP",
                        gstNumberVerified == null ? "null" : gstNumberVerified ? "true" : "false",
                        shopNumber, shopImage),

                new KycDocumentDto("BUSINESS",
                        gstNumberVerified == null ? "null" : gstNumberVerified ? "true" : "false",
                        businessNumber, businessProofImage),

                new KycDocumentDto("AADHAAR_PAN_VERIFIED",
                        aadhaarPanVerified == null ? "null" : aadhaarPanVerified ? "true" : "false",
                        aadhaarNumber, null)
        );
    }



}


