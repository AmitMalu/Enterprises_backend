package com.im.admin.service.impl;

import com.im.admin.dto.KYCEditRequest;
import com.im.admin.dto.KYCRequestDto;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.response.KYCApprovedResponseDto;
import com.im.admin.response.KYCResponseDto;
import com.im.admin.response.KycDocumentDto;
import com.im.admin.service.KYCService;
import com.im.admin.util.CommonMethods;
import jakarta.transaction.Transactional;
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


    @Override
    public KYCResponseDto searchByMobileNoAndHierarchyId(KYCRequestDto kycRequestDto) {

        String searchKeyword = kycRequestDto.getSearchKeyword();

        // Try to extract type (RT, DT, PT, etc.)
        String type = commonMethods.extractHierarchyType(searchKeyword);

        if (type != null && !"UNKNOWN".equalsIgnoreCase(type)) {
            return getByHierarchyId(type, searchKeyword);
        } else {
            return getByMobileNumber(searchKeyword);
        }
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
//                r.getEmailId(), r.getEmailVerified(),
//                r.getAadhaarCardFrontImage(), r.getAadhaarCardBackImage(), r.getIsAadhaarVerified(), r.getAadhaarNumber(),
//                r.getPanCardImage(), r.getPanNumber(), r.getPanVerified(), r.getAadhaarPanVerified(),
//                r.getShopImage(), r.getShopNumber(), r.getBusinessProofImage(), r.getBusinessNumber(), r.getGstNumberVerified()
        );
    }

    private KYCResponseDto mapDistributor(Distributor d) {
        return new KYCResponseDto(
                d.getDistributorId(), d.getName(), d.getMobileNumber(),
                d.getEmailId()
//                d.getEmailId(), d.getEmailVerified(),
//                d.getAadhaarCardFrontImage(), d.getAadhaarCardBackImage(), d.getIsAadhaarVerified(), d.getAadhaarNumber(),
//                d.getPanCardImage(), d.getPanNumber(), d.getPanVerified(), d.getAadhaarPanVerified(),
//                d.getShopImage(), d.getShopNumber(), d.getBusinessProofImage(), d.getBusinessNumber(), d.getGstNumberVerified()
        );
    }

    private KYCResponseDto mapPartner(Partner p) {
        return new KYCResponseDto(
                p.getPartnerId(), p.getName(), p.getMobileNumber(),
                p.getEmailId()
//                p.getEmailId(), p.getEmailVerified(),
//                p.getAadhaarCardFrontImage(), p.getAadhaarCardBackImage(), p.getIsAadhaarVerified(), p.getAadhaarNumber(),
//                p.getPanCardImage(), p.getPanNumber(), p.getPanVerified(), p.getAadhaarPanVerified(),
//                p.getShopImage(), p.getShopNumber(), p.getBusinessProofImage(), p.getBusinessNumber(), p.getGstNumberVerified()
        );
    }

    private KYCResponseDto mapSuperDistributor(SuperDistributor sd) {
        return new KYCResponseDto(
                sd.getSuperDistributorId(), sd.getName(), sd.getMobileNumber(),
                 sd.getEmailId()
//                sd.getEmailId(), sd.getEmailVerified(),
//                sd.getAadhaarCardFrontImage(), sd.getAadhaarCardBackImage(), sd.getIsAadhaarVerified(), sd.getAadhaarNumber(),
//                sd.getPanCardImage(), sd.getPanNumber(), sd.getPanVerified(), sd.getAadhaarPanVerified(),
//                sd.getShopImage(), sd.getShopNumber(), sd.getBusinessProofImage(), sd.getBusinessNumber(), sd.getGstNumberVerified()
        );
    }

    private KYCResponseDto mapWhiteLabel(WhiteLabels wl) {
        return new KYCResponseDto(
                wl.getWhiteLabelId(), wl.getTitle(), wl.getMobileNumber(),
                 wl.getEmailId()
//                wl.getEmailId(), wl.getEmailVerified(),
//                wl.getAadhaarCardFrontImage(), wl.getAadhaarCardBackImage(), wl.getIsAadhaarVerified(), wl.getAadhaarNumber(),
//                wl.getPanCardImage(), wl.getPanNumber(), wl.getPanVerified(), wl.getAadhaarPanVerified(),
//                wl.getShopImage(), wl.getShopNumber(), wl.getBusinessProofImage(), wl.getBusinessNumber(), wl.getGstNumberVerified()
        );
    }

    private KYCResponseDto mapEnterprise(Enterprises et) {
        return new KYCResponseDto(
                et.getEnterpriseId(), et.getName(), et.getMobileNumber(),
                et.getEmailId()
//                et.getEmailId(), et.getEmailVerified(),
//                et.getAadhaarCardFrontImage(), et.getAadhaarCardBackImage(), et.getIsAadhaarVerified(), et.getAadhaarNumber(),
//                et.getPanCardImage(), et.getPanNumber(), et.getPanVerified(), et.getAadhaarPanVerified(),
//                et.getShopImage(), et.getShopNumber(), et.getBusinessProofImage(), et.getBusinessNumber(), et.getGstNumberVerified()
        );
    }

    @Override
    public Page<KYCResponseDto> searchByHierarchyWise(String hierarchyType, Pageable pageable) {
        return switch (hierarchyType.toUpperCase()) {
            case "RT" -> retailerRepository.findAll(pageable)
                    .map(this::mapRetailer);

            case "DT" -> distributorRepository.findAll(pageable)
                    .map(this::mapDistributor);

            case "PT" -> partnerRepository.findAll(pageable)
                    .map(this::mapPartner);

            case "SD" -> superDistributorRepository.findAll(pageable)
                    .map(this::mapSuperDistributor);

            case "WL" -> whiteLabelsRepository.findAll(pageable)
                    .map(this::mapWhiteLabel);

            case "ET" -> enterprisesRepository.findAll(pageable)
                    .map(this::mapEnterprise);

            default -> Page.empty(pageable);
        };
    }

    @Override
    public String editKyc(KYCEditRequest request, MultipartFile imageFile) {
        String imagePath = null;

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String folder = "KYC_" + request.getCategory().toUpperCase();
                imagePath = saveImage(imageFile, folder);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to save image: " + e.getMessage();
        }

        String hierarchyId = request.getHierarchyId();
        String type = commonMethods.extractHierarchyType(request.getHierarchyId());
        String category = request.getCategory();
        boolean verified = request.isVerified();


        final String finalImagePath = imagePath;

        switch (type) {
            case "RT" -> retailerRepository.findById(hierarchyId).ifPresent(r -> {
                updateDocument(r, category, verified, finalImagePath);
                retailerRepository.save(r);
            });
            case "DT" -> distributorRepository.findById(hierarchyId).ifPresent(d -> {
                updateDocument(d, category, verified, finalImagePath);
                distributorRepository.save(d);
            });
            case "PT" -> partnerRepository.findById(hierarchyId).ifPresent(p -> {
                updateDocument(p, category, verified, finalImagePath);
                partnerRepository.save(p);
            });
            case "SD" -> superDistributorRepository.findById(hierarchyId).ifPresent(sd -> {
                updateDocument(sd, category, verified, finalImagePath);
                superDistributorRepository.save(sd);
            });
            case "WL" -> whiteLabelsRepository.findById(hierarchyId).ifPresent(wl -> {
                updateDocument(wl, category, verified, finalImagePath);
                whiteLabelsRepository.save(wl);
            });
            case "ET" -> enterprisesRepository.findById(hierarchyId).ifPresent(et -> {
                updateDocument(et, category, verified, finalImagePath);
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


    private void updateDocument(Object entity, String category, boolean verified, String image) {
        switch (category.toUpperCase()) {
            case "AADHAAR_FRONT" -> {
                if (entity instanceof Retailer r) {
                    if (image != null) r.setAadhaarCardFrontImage(image);
                    r.setIsAadhaarVerified(verified);
                } else if (entity instanceof Distributor d) {
                    if (image != null) d.setAadhaarCardFrontImage(image);
                    d.setIsAadhaarVerified(verified);
                } else if (entity instanceof Partner p) {
                    if (image != null) p.setAadhaarCardFrontImage(image);
                    p.setIsAadhaarVerified(verified);
                } else if (entity instanceof SuperDistributor sd) {
                    if (image != null) sd.setAadhaarCardFrontImage(image);
                    sd.setIsAadhaarVerified(verified);
                } else if (entity instanceof WhiteLabels wl) {
                    if (image != null) wl.setAadhaarCardFrontImage(image);
                    wl.setIsAadhaarVerified(verified);
                } else if (entity instanceof Enterprises et) {
                    if (image != null) et.setAadhaarCardFrontImage(image);
                    et.setIsAadhaarVerified(verified);
                }
            }

            case "AADHAAR_BACK" -> {
                if (entity instanceof Retailer r) {
                    if (image != null) r.setAadhaarCardBackImage(image);
                    r.setIsAadhaarVerified(verified);
                } else if (entity instanceof Distributor d) {
                    if (image != null) d.setAadhaarCardBackImage(image);
                    d.setIsAadhaarVerified(verified);
                } else if (entity instanceof Partner p) {
                    if (image != null) p.setAadhaarCardBackImage(image);
                    p.setIsAadhaarVerified(verified);
                } else if (entity instanceof SuperDistributor sd) {
                    if (image != null) sd.setAadhaarCardBackImage(image);
                    sd.setIsAadhaarVerified(verified);
                } else if (entity instanceof WhiteLabels wl) {
                    if (image != null) wl.setAadhaarCardBackImage(image);
                    wl.setIsAadhaarVerified(verified);
                } else if (entity instanceof Enterprises et) {
                    if (image != null) et.setAadhaarCardBackImage(image);
                    et.setIsAadhaarVerified(verified);
                }
            }

            case "PAN" -> {
                if (entity instanceof Retailer r) {
                    if (image != null) r.setPanCardImage(image);
                    r.setPanVerified(verified);
                } else if (entity instanceof Distributor d) {
                    if (image != null) d.setPanCardImage(image);
                    d.setPanVerified(verified);
                } else if (entity instanceof Partner p) {
                    if (image != null) p.setPanCardImage(image);
                    p.setPanVerified(verified);
                } else if (entity instanceof SuperDistributor sd) {
                    if (image != null) sd.setPanCardImage(image);
                    sd.setPanVerified(verified);
                } else if (entity instanceof WhiteLabels wl) {
                    if (image != null) wl.setPanCardImage(image);
                    wl.setPanVerified(verified);
                } else if (entity instanceof Enterprises et) {
                    if (image != null) et.setPanCardImage(image);
                    et.setPanVerified(verified);
                }
            }

            case "SHOP" -> {
                if (entity instanceof Retailer r) {
                    if (image != null) r.setShopImage(image);
                } else if (entity instanceof Distributor d) {
                    if (image != null) d.setShopImage(image);
                } else if (entity instanceof Partner p) {
                    if (image != null) p.setShopImage(image);
                } else if (entity instanceof SuperDistributor sd) {
                    if (image != null) sd.setShopImage(image);
                } else if (entity instanceof WhiteLabels wl) {
                    if (image != null) wl.setShopImage(image);
                } else if (entity instanceof Enterprises et) {
                    if (image != null) et.setShopImage(image);
                }
            }

            case "BUSINESS" -> {
                if (entity instanceof Retailer r) {
                    if (image != null) r.setBusinessProofImage(image);
                    r.setGstNumberVerified(verified);
                } else if (entity instanceof Distributor d) {
                    if (image != null) d.setBusinessProofImage(image);
                    d.setGstNumberVerified(verified);
                } else if (entity instanceof Partner p) {
                    if (image != null) p.setBusinessProofImage(image);
                    p.setGstNumberVerified(verified);
                } else if (entity instanceof SuperDistributor sd) {
                    if (image != null) sd.setBusinessProofImage(image);
                    sd.setGstNumberVerified(verified);
                } else if (entity instanceof WhiteLabels wl) {
                    if (image != null) wl.setBusinessProofImage(image);
                    wl.setGstNumberVerified(verified);
                } else if (entity instanceof Enterprises et) {
                    if (image != null) et.setBusinessProofImage(image);
                    et.setGstNumberVerified(verified);
                }
            }

            case "EMAIL" -> {
                if (entity instanceof Retailer r) {
                    r.setEmailVerified(verified);
                } else if (entity instanceof Distributor d) {
                    d.setEmailVerified(verified);
                } else if (entity instanceof Partner p) {
                    p.setEmailVerified(verified);
                } else if (entity instanceof SuperDistributor sd) {
                    sd.setEmailVerified(verified);
                } else if (entity instanceof WhiteLabels wl) {
                    wl.setEmailVerified(verified);
                } else if (entity instanceof Enterprises et) {
                    et.setEmailVerified(verified);
                }
            }

            case "AADHAAR_PAN_VERIFIED" -> {
                if (entity instanceof Retailer r) {
                    r.setAadhaarPanVerified(verified);
                } else if (entity instanceof Distributor d) {
                    d.setAadhaarPanVerified(verified);
                } else if (entity instanceof Partner p) {
                    p.setAadhaarPanVerified(verified);
                } else if (entity instanceof SuperDistributor sd) {
                    sd.setAadhaarPanVerified(verified);
                } else if (entity instanceof WhiteLabels wl) {
                    wl.setAadhaarPanVerified(verified);
                } else if (entity instanceof Enterprises et) {
                    et.setAadhaarPanVerified(verified);
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
    public Object getApprovedDocuments(String searchKeyword, Pageable pageable) {

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
                    return switch (type) {
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
                } else {
                    // Fallback search by mobile number
                    Retailer r = retailerRepository.findByMobileNumber(searchKeyword);
                    if (r != null && hasApprovedDocuments(r)) return mapRetailer(r);

                    Distributor d = distributorRepository.findByMobileNumber(searchKeyword);
                    if (d != null && hasApprovedDocuments(d)) return mapDistributor(d);

                    Partner p = partnerRepository.findByMobileNumber(searchKeyword);
                    if (p != null && hasApprovedDocuments(p)) return mapPartner(p);

                    SuperDistributor sd = superDistributorRepository.findByMobileNumber(searchKeyword);
                    if (sd != null && hasApprovedDocuments(sd)) return mapSuperDistributor(sd);

                    WhiteLabels wl = whiteLabelsRepository.findByMobileNumber(searchKeyword);
                    if (wl != null && hasApprovedDocuments(wl)) return mapWhiteLabel(wl);

                    Enterprises et = enterprisesRepository.findByMobileNumber(searchKeyword);
                    if (et != null && hasApprovedDocuments(et)) return mapEnterprise(et);

                    return null;
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
            aadhaarFront = r.getAadhaarCardFrontImage();
            aadhaarBack = r.getAadhaarCardBackImage();
            panNumber = r.getPanNumber();
            panImage = r.getPanCardImage();
            shopNumber = r.getShopNumber();
            shopImage = r.getShopImage();
            businessNumber = r.getBusinessNumber();
            businessProofImage = r.getBusinessProofImage();
            isEmailVerified = r.getEmailVerified();
            isAadhaarVerified = r.getIsAadhaarVerified();
            aadhaarPanVerified = r.getAadhaarPanVerified();
            gstNumberVerified = r.getGstNumberVerified();
            panVerified = r.getPanVerified();
        }
        else if (entity instanceof Distributor d) {
            email = d.getEmailId();
            aadhaarNumber = d.getAadhaarNumber();
            aadhaarFront = d.getAadhaarCardFrontImage();
            aadhaarBack = d.getAadhaarCardBackImage();
            panNumber = d.getPanNumber();
            panImage = d.getPanCardImage();
            shopNumber = d.getShopNumber();
            shopImage = d.getShopImage();
            businessNumber = d.getBusinessNumber();
            businessProofImage = d.getBusinessProofImage();
            isEmailVerified = d.getEmailVerified();
            isAadhaarVerified = d.getIsAadhaarVerified();
            aadhaarPanVerified = d.getAadhaarPanVerified();
            gstNumberVerified = d.getGstNumberVerified();
            panVerified = d.getPanVerified();
        }
        else if (entity instanceof Partner p) {
            email = p.getEmailId();
            aadhaarNumber = p.getAadhaarNumber();
            aadhaarFront = p.getAadhaarCardFrontImage();
            aadhaarBack = p.getAadhaarCardBackImage();
            panNumber = p.getPanNumber();
            panImage = p.getPanCardImage();
            shopNumber = p.getShopNumber();
            shopImage = p.getShopImage();
            businessNumber = p.getBusinessNumber();
            businessProofImage = p.getBusinessProofImage();
            isEmailVerified = p.getEmailVerified();
            isAadhaarVerified = p.getIsAadhaarVerified();
            aadhaarPanVerified = p.getAadhaarPanVerified();
            gstNumberVerified = p.getGstNumberVerified();
            panVerified = p.getPanVerified();
        }
        else if (entity instanceof SuperDistributor sd) {
            email = sd.getEmailId();
            aadhaarNumber = sd.getAadhaarNumber();
            aadhaarFront = sd.getAadhaarCardFrontImage();
            aadhaarBack = sd.getAadhaarCardBackImage();
            panNumber = sd.getPanNumber();
            panImage = sd.getPanCardImage();
            shopNumber = sd.getShopNumber();
            shopImage = sd.getShopImage();
            businessNumber = sd.getBusinessNumber();
            businessProofImage = sd.getBusinessProofImage();
            isEmailVerified = sd.getEmailVerified();
            isAadhaarVerified = sd.getIsAadhaarVerified();
            aadhaarPanVerified = sd.getAadhaarPanVerified();
            gstNumberVerified = sd.getGstNumberVerified();
            panVerified = sd.getPanVerified();
        }
        else if (entity instanceof WhiteLabels wl) {
            email = wl.getEmailId();
            aadhaarNumber = wl.getAadhaarNumber();
            aadhaarFront = wl.getAadhaarCardFrontImage();
            aadhaarBack = wl.getAadhaarCardBackImage();
            panNumber = wl.getPanNumber();
            panImage = wl.getPanCardImage();
            shopNumber = wl.getShopNumber();
            shopImage = wl.getShopImage();
            businessNumber = wl.getBusinessNumber();
            businessProofImage = wl.getBusinessProofImage();
            isEmailVerified = wl.getEmailVerified();
            isAadhaarVerified = wl.getIsAadhaarVerified();
            aadhaarPanVerified = wl.getAadhaarPanVerified();
            gstNumberVerified = wl.getGstNumberVerified();
            panVerified = wl.getPanVerified();
        }
        else if (entity instanceof Enterprises et) {
            email = et.getEmailId();
            aadhaarNumber = et.getAadhaarNumber();
            aadhaarFront = et.getAadhaarCardFrontImage();
            aadhaarBack = et.getAadhaarCardBackImage();
            panNumber = et.getPanNumber();
            panImage = et.getPanCardImage();
            shopNumber = et.getShopNumber();
            shopImage = et.getShopImage();
            businessNumber = et.getBusinessNumber();
            businessProofImage = et.getBusinessProofImage();
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


