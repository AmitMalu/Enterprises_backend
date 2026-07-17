package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.SubProductDropdownResponseDto;
import com.im.admin.dto.SubProductsResponseDTO;
import com.im.admin.dto.SubproductDto;
import com.im.admin.entities.AccessHierarchyNode;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.enums.SubproductServiceType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.AccessHierarchyNodeRepository;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.request.SubproductListRequest;
import com.im.admin.response.SubproductListResponse;
import com.im.admin.service.SubProductsService;
import com.im.admin.spec.SubProductSpecification;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SubProductsServiceImpl implements SubProductsService {

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private AccessHierarchyNodeRepository accessHierarchyNodeRepository;

    private static final String PG_SUBPRODUCT_PATH =
            "/services/paymentgatwayAPI/subproduct-list";

    private static final String PAYOUT_SUBPRODUCT_PATH =
            "/services/payoutAPI/subproduct-list";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${instantmudra.base-url}")
    private String baseUrl;

    @Value("${instantmudra.authorization-token}")
    private String authorizationToken;

    @Override
    public SubProducts saveSubProduct(SubProducts subProduct) {

        Products product = productsRepository.findById(subProduct.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid productId: " + subProduct.getProduct().getId()));

        subProduct.setProduct(product);

        //  Generate ID first
        String nextId = commonMethods.generateNextIdForTables("sub_products", "id");
        subProduct.setId(nextId);

        //  Set subProductCode = id (same like Product logic)
        subProduct.setSubProductCode(nextId);


        Optional<SubProducts> existingSubProduct = subProductsRepository
                .findBySubProductCodeOrSubProductTitle(subProduct.getSubProductCode(), subProduct.getSubProductTitle());

        if (existingSubProduct.isPresent()) {
            SubProducts existing = existingSubProduct.get();
            if (existing.getSubProductCode().equals(subProduct.getSubProductCode())) {
                throw new IllegalArgumentException("Sub Product Code '" + subProduct.getSubProductCode() + "' is already in use.");
            }
            if (existing.getSubProductTitle().equals(subProduct.getSubProductTitle())) {
                throw new IllegalArgumentException("Sub Product Title '" + subProduct.getSubProductTitle() + "' is already in use.");
            }
        }

//        subProduct.setId(commonMethods.generateNextIdForTables("sub_products", "id"));

        if (!Boolean.TRUE.equals(subProduct.getIsSettlement())) {
            subProduct.setIsSettlement(false);
            subProduct.setSettlementTime("-");
        }

        if (!Boolean.TRUE.equals(subProduct.getIsWhiteLabelAdmin())) {
            subProduct.setIsWhiteLabelAdmin(false);
            subProduct.setWhiteLabelAdminId("-");
        }

        return subProductsRepository.save(subProduct);
    }


    @Override
    @Transactional
    public void deleteSubProduct(String id) {
        if (!subProductsRepository.existsById(id)) {
            throw new RuntimeException("SubProduct with ID " + id + " not found.");
        }
        subProductsRepository.deleteById(id);
        log.info("Deleted SubProduct with ID: {}", id);
    }

    @Override
    @Transactional
    public SubProducts updateSubProduct(String id, SubProducts subProduct) {
        SubProducts existingSubProduct = subProductsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SubProduct with ID " + id + " not found."));

        // Check if productId is changed and fetch product
        if (!Objects.equals(existingSubProduct.getProduct().getId(), subProduct.getProduct().getId())) {
            Products product = productsRepository.findById(subProduct.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid productId: " + subProduct.getProduct().getId()));
            existingSubProduct.setProduct(product);
        }

        existingSubProduct.setSubProductTitle(subProduct.getSubProductTitle());
//        existingSubProduct.setSubProductCode(subProduct.getSubProductCode()); // ******chnage code****

//        existingSubProduct.setProductId(subProduct.getProductId());
        existingSubProduct.setIsSettlement(subProduct.getIsSettlement());
        existingSubProduct.setWalletType(subProduct.getWalletType());
        existingSubProduct.setIsVisible(subProduct.getIsVisible());
        existingSubProduct.setIsBlocked(subProduct.getIsBlocked());
        existingSubProduct.setEditedBy(subProduct.getEditedBy());
        existingSubProduct.setIsActive(subProduct.getIsActive());

        if (Boolean.TRUE.equals(subProduct.getIsSettlement())) {
            existingSubProduct.setIsSettlement(true);
            existingSubProduct.setSettlementTime(subProduct.getSettlementTime());
        } else {
            existingSubProduct.setIsSettlement(false);
            existingSubProduct.setSettlementTime("-");
        }

        if (Boolean.TRUE.equals(subProduct.getIsWhiteLabelAdmin())) {
            existingSubProduct.setIsWhiteLabelAdmin(true);
            existingSubProduct.setWhiteLabelAdminId(subProduct.getWhiteLabelAdminId());
        } else {
            existingSubProduct.setIsWhiteLabelAdmin(false);
            existingSubProduct.setWhiteLabelAdminId("-");
        }

        SubProducts updatedSubProduct = subProductsRepository.save(existingSubProduct);
        log.info("Updated SubProduct with ID: {}", id);
        return updatedSubProduct;
    }

    @Override
    public SubProductsResponseDTO getSubProductById(String id) {
        SubProducts subProduct = subProductsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubProduct with ID " + id + " not found."));

        return toDTO(subProduct);
    }

    @Override
    public Page<SubProductsResponseDTO> getAllSubProducts(Pageable pageable, List<String> id,
                                                          String subProductTitle, String subProductCode, String productCode,
                                                          Short productId, String productName, String walletType,
                                                          Boolean isSettlement, Boolean isActive, Boolean enabled,
                                                          String createdBy, String editedBy,
                                                          LocalDate createdOn, LocalDate editedOn) {

        Specification<SubProducts> spec = SubProductSpecification.getFilter(
                id, subProductTitle, subProductCode, productCode, productId,
                productName, walletType, isSettlement, isActive, enabled,
                createdBy, editedBy, createdOn, editedOn
        );

        Page<SubProducts> subProductEntities = subProductsRepository.findAll(spec, pageable);

        return subProductEntities.map(this::toDTO);
    }


    @Override
    public List<SubProductDropdownResponseDto>
    getActiveSubProductsByProductId(Short productId, Boolean isSettlement) {

        long start = System.currentTimeMillis();

        try {

            String loginHierarchy = tokenUtil.extractHierarchyId();

            log.info("[SUB PRODUCT] login={} product={} settlement={}", loginHierarchy, productId, isSettlement);

            AccessHierarchyNode node =

                    accessHierarchyNodeRepository.findById(loginHierarchy)
                            .orElseThrow(() -> new RuntimeException("Hierarchy not found"));

            String type = node.getHierarchyType();

            List<SubProducts> result;

            /*
             * AD
             * AU under AD
             */

            if (

                    "AD".equalsIgnoreCase(type)

                            ||

                            (

                                    type.endsWith("AU")

                                            &&

                                            node
                                                    .getParentHierarchyId()

                                                    .contains(
                                                            "-AD-"
                                                    )

                            )

            ) {

                log.info("[SUB PRODUCT] FULL ACCESS");

                result = isSettlement == null

                        ?

                        subProductsRepository
                                .findByProductIdAndIsActiveTrue(
                                        productId
                                )

                        :

                        subProductsRepository
                                .findByProductIdAndIsActiveTrueAndIsSettlement(
                                        productId,
                                        isSettlement
                                );

            }

            /*
             * WL
             */

            else if (

                    "WL".equalsIgnoreCase(type)

            ) {

                log.info("[SUBPRODUCT] WL ACCESS");

                result =

                        subProductsRepository
                                .findWLSubProducts(productId, isSettlement, loginHierarchy);

            }

            /*
             * WLAU
             */

            else if (

                    type.endsWith("AU")

            ) {

                String parent = node.getParentHierarchyId();

                log.info("[SUB PRODUCT] WLAU parent={}", parent
                );

                result =

                        subProductsRepository
                                .findWLSubProducts(

                                        productId,

                                        isSettlement,

                                        parent
                                );

            } else {

                result = Collections.emptyList();

            }

            log.info(
                    "[SUB PRODUCT] returned={} time={}ms", result.size(), System.currentTimeMillis() - start);

            return result

                    .stream()

                    .map(

                            x ->

                                    new SubProductDropdownResponseDto(

                                            x.getId(),

                                            x.getSubProductTitle(),

                                            x.getSubProductCode()
                                    )
                    )

                    .toList();

        } catch (

                Exception ex

        ) {

            log.error(
                    "[SUB PRODUCT] ERROR",
                    ex
            );

            throw new RuntimeException("Failed to fetch sub products");

        }

    }

    public SubProductsResponseDTO toDTO(SubProducts entity) {
        SubProductsResponseDTO dto = new SubProductsResponseDTO();
        dto.setId(entity.getId());
        dto.setSubProductTitle(entity.getSubProductTitle());
        dto.setSubProductCode(entity.getSubProductCode());
        dto.setWalletType(entity.getWalletType());
        dto.setIsSettlement(entity.getIsSettlement());
        dto.setSettlementTime(entity.getSettlementTime());
        dto.setIsActive(entity.getIsActive());
        dto.setIsVisible(entity.getIsVisible());
        dto.setIsBlocked(entity.getIsBlocked());
        dto.setIsWhiteLabelAdmin(entity.getIsWhiteLabelAdmin());
        dto.setWhiteLabelAdminId(entity.getWhiteLabelAdminId());
        dto.setUserField1(entity.getUserField1());
        dto.setUserField2(entity.getUserField2());
        dto.setUserField3(entity.getUserField3());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setEditedBy(entity.getEditedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setEditedOn(entity.getEditedOn());

        if (entity.getProduct() != null) {
            dto.setProductId(entity.getProduct().getId());
            dto.setProductCode(entity.getProduct().getProductCode());
            dto.setProductName(entity.getProduct().getName());
        }

        return dto;
    }

    @Override
    public List<SubProductDropdownResponseDto> getSubProductDropdown() {
        List<SubProducts> allSubProducts = subProductsRepository.findAll();

        return allSubProducts.stream()
                .map(sp -> new SubProductDropdownResponseDto(sp.getId(), sp.getSubProductTitle()))
                .collect(Collectors.toList());
    }

    @Override
    public SubproductListResponse getSubProductList(
            SubproductListRequest request, String geoLocation
    ) {

        validateRequest(request);

        SubproductServiceType serviceType =
                parseServiceType(request.getServiceType());

        String vendorPath =
                resolveVendorPath(serviceType);

        String vendorUrl =
                buildUrl(baseUrl, vendorPath);

        Map<String, String> vendorRequestBody =
                Map.of(
                        "geolocation",
                        geoLocation
                );

        HttpHeaders headers =
                createVendorHeaders();

        HttpEntity<Map<String, String>> httpEntity =
                new HttpEntity<>(
                        vendorRequestBody,
                        headers
                );

        log.info(
                "[SUBPRODUCT LIST] Calling vendor API | serviceType={} | url={} | geolocation={}",
                serviceType,
                vendorUrl,
                geoLocation
        );

        try {

            ResponseEntity<String> vendorResponse =
                    restTemplate.exchange(
                            vendorUrl,
                            HttpMethod.POST,
                            httpEntity,
                            String.class
                    );

            log.info(
                    "[SUBPRODUCT LIST] Vendor response received | serviceType={} | httpStatus={} | response={}",
                    serviceType,
                    vendorResponse.getStatusCode(),
                    vendorResponse.getBody()
            );

            return parseVendorResponse(
                    vendorResponse.getBody(),
                    serviceType
            );

        } catch (HttpStatusCodeException ex) {

            log.error(
                    "[SUBPRODUCT LIST] Vendor HTTP error | serviceType={} | status={} | response={}",
                    serviceType,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    ex
            );

            return parseErrorResponse(
                    ex.getResponseBodyAsString(),
                    "Vendor returned an error"
            );

        } catch (RestClientException ex) {

            log.error(
                    "[SUBPRODUCT LIST] Unable to communicate with vendor | serviceType={}",
                    serviceType,
                    ex
            );

            return failedResponse(
                    "Unable to communicate with vendor"
            );

        } catch (Exception ex) {

            log.error(
                    "[SUBPRODUCT LIST] Unexpected error | serviceType={}",
                    serviceType,
                    ex
            );

            return failedResponse(
                    "Something went wrong while fetching subproduct list"
            );
        }
    }



    private SubproductListResponse parseVendorResponse(
            String responseBody,
            SubproductServiceType serviceType
    ) {

        if (responseBody == null || responseBody.isBlank()) {

            log.warn(
                    "[SUBPRODUCT LIST] Empty vendor response | serviceType={}",
                    serviceType
            );

            return failedResponse(
                    "Empty response received from vendor"
            );
        }

        try {

            JsonNode rootNode =
                    objectMapper.readTree(responseBody);

            String status =
                    getTextValue(
                            rootNode,
                            "status"
                    );

            String message =
                    getTextValue(
                            rootNode,
                            "msg"
                    );

            List<SubproductDto> subproducts =
                    new ArrayList<>();

            JsonNode dataNode =
                    rootNode.get("data");

            if (dataNode != null && dataNode.isArray()) {

                for (JsonNode item : dataNode) {

                    SubproductDto subproduct =
                            SubproductDto.builder()
                                    .title(
                                            getTextValue(
                                                    item,
                                                    "title"
                                            )
                                    )
                                    .subproduct(
                                            getTextValue(
                                                    item,
                                                    "subproduct"
                                            )
                                    )
                                    .build();

                    subproducts.add(subproduct);
                }
            }

            if (status == null || status.isBlank()) {
                status = "error";
            }

            if (message == null || message.isBlank()) {

                message =
                        "success".equalsIgnoreCase(status)
                                ? "Subproduct list fetched successfully"
                                : "Error while fetching subproduct list";
            }

            log.info(
                    "[SUBPRODUCT LIST] Vendor response parsed | serviceType={} | status={} | recordCount={}",
                    serviceType,
                    status,
                    subproducts.size()
            );

            return SubproductListResponse.builder()
                    .status(status)
                    .msg(message)
                    .data(subproducts)
                    .build();

        } catch (Exception ex) {

            log.error(
                    "[SUBPRODUCT LIST] Failed to parse vendor response | serviceType={} | response={}",
                    serviceType,
                    responseBody,
                    ex
            );

            return failedResponse(
                    "Invalid response received from vendor"
            );
        }
    }

    private SubproductListResponse parseErrorResponse(
            String responseBody,
            String defaultMessage
    ) {

        if (responseBody == null || responseBody.isBlank()) {
            return failedResponse(defaultMessage);
        }

        try {

            JsonNode rootNode =
                    objectMapper.readTree(responseBody);

            String message =
                    getTextValue(
                            rootNode,
                            "msg"
                    );

            if (message == null || message.isBlank()) {
                message = defaultMessage;
            }

            return failedResponse(message);

        } catch (Exception ex) {

            log.warn(
                    "[SUBPRODUCT LIST] Unable to parse vendor error response | response={}",
                    responseBody
            );

            return failedResponse(defaultMessage);
        }
    }

    private String getTextValue(
            JsonNode node,
            String fieldName
    ) {

        if (node == null) {
            return null;
        }

        JsonNode fieldNode =
                node.get(fieldName);

        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }

        return fieldNode.asText();
    }

    private HttpHeaders createVendorHeaders() {

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.setAccept(
                Collections.singletonList(
                        MediaType.APPLICATION_JSON
                )
        );

        headers.setBearerAuth(
                normalizeAuthorizationToken(
                        authorizationToken
                )
        );

        return headers;
    }

    private String resolveVendorPath(
            SubproductServiceType serviceType
    ) {

        return switch (serviceType) {

            case PG ->
                    PG_SUBPRODUCT_PATH;

            case PAYOUT ->
                    PAYOUT_SUBPRODUCT_PATH;
        };
    }

    private SubproductServiceType parseServiceType(
            String serviceType
    ) {

        if (serviceType == null || serviceType.isBlank()) {

            throw new IllegalArgumentException(
                    "Service type is required. Allowed values are PG and PAYOUT"
            );
        }

        try {

            return SubproductServiceType.valueOf(
                    serviceType
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException ex) {

            throw new IllegalArgumentException(
                    "Invalid service type. Allowed values are PG and PAYOUT"
            );
        }
    }

    private void validateRequest(
            SubproductListRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Request body is required"
            );
        }

        if (request.getServiceType() == null
                || request.getServiceType().isBlank()) {

            throw new IllegalArgumentException(
                    "Service type is required. Allowed values are PG and PAYOUT"
            );
        }
    }

    private String normalizeGeolocation(
            String geolocation
    ) {

        String normalized =
                geolocation
                        .trim()
                        .replaceAll("\\s+", "");

        if (!normalized.matches(
                "^-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?$"
        )) {

            throw new IllegalArgumentException(
                    "Invalid geolocation format. Expected latitude,longitude"
            );
        }

        String[] coordinates =
                normalized.split(",");

        double latitude =
                Double.parseDouble(coordinates[0]);

        double longitude =
                Double.parseDouble(coordinates[1]);

        if (latitude < -90 || latitude > 90) {

            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90"
            );
        }

        if (longitude < -180 || longitude > 180) {

            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180"
            );
        }

        return normalized;
    }

    private String normalizeAuthorizationToken(
            String token
    ) {

        if (token == null || token.isBlank()) {

            throw new IllegalStateException(
                    "InstantMudra authorization token is not configured"
            );
        }

        String normalizedToken =
                token.trim();

        if (normalizedToken
                .toLowerCase(Locale.ROOT)
                .startsWith("bearer ")) {

            return normalizedToken
                    .substring(7)
                    .trim();
        }

        return normalizedToken;
    }

    private String buildUrl(
            String baseUrl,
            String path
    ) {

        if (baseUrl == null || baseUrl.isBlank()) {

            throw new IllegalStateException(
                    "InstantMudra base URL is not configured"
            );
        }

        String normalizedBaseUrl =
                baseUrl.trim();

        while (normalizedBaseUrl.endsWith("/")) {

            normalizedBaseUrl =
                    normalizedBaseUrl.substring(
                            0,
                            normalizedBaseUrl.length() - 1
                    );
        }

        String normalizedPath =
                path.startsWith("/")
                        ? path
                        : "/" + path;

        return normalizedBaseUrl + normalizedPath;
    }

    private SubproductListResponse failedResponse(
            String message
    ) {

        return SubproductListResponse.builder()
                .status("error")
                .msg(message)
                .data(Collections.emptyList())
                .build();
    }


}

