package com.im.admin.controller;


import com.im.admin.dto.*;
import com.im.admin.entities.ImMaster;
import com.im.admin.entities.ImSubMaster;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.MasterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/master")
public class MasterController {

    @Autowired
    private MasterService masterService;

    @PostMapping("/card-type")
    public ResponseEntity<CommonResponse<?>> cardType() {
        log.info("API Call: cardType");
        return null;
    }

    @PostMapping
    public ResponseEntity<CommonResponse> addMaster(@RequestBody MasterRequestDto masterRequestDto) {
        log.info("API Call: addMaster with request: {}", masterRequestDto);
        String result = masterService.addMaster(masterRequestDto);

        if ("Master already exists.".equals(result)) {
            log.warn("Master already exists: {}", masterRequestDto);
            return ResponseEntity
                    .status(409) // Conflict
                    .body(new CommonResponse(409, result, null));
        }
        log.info("Master added successfully: {}", masterRequestDto);
        return ResponseEntity
                .ok(new CommonResponse(201, result, null));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<MasterResponseDto>>> getAllMasters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer imMasterId,
            @RequestParam(required = false) String imMasterName,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate createdOn
    ) {
        log.info("API Call: getAllMasters with page: {}, size: {}, imMasterId: {}, imMasterName: {}, createdBy: {}, createdOn: {}",
                page, size, imMasterId, imMasterName, createdBy, createdOn);

        Page<MasterResponseDto> result =
                masterService.getAllMasters(page, size, imMasterId, imMasterName, createdBy, createdOn);

        log.info("Fetched all masters successfully, count={}", result.getTotalElements());
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", result));
    }


    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<MasterResponseDto>> getMasterById(@PathVariable Integer id) {
        log.info("API Call: getMasterById with id: {}", id);
        MasterResponseDto dto = masterService.getMasterById(id);
        log.info("Fetched master successfully for id: {}", id);
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", dto));
    }

    @GetMapping("/dropdown")
    public ResponseEntity<CommonResponse> getAllMasterDropdown(){
        log.info("API Call: getAllMasterDropdown");
        List<MasterResponseDto> allMasterDropdown = masterService.getAllMasterDropdown();
        log.info("Fetched master dropdown successfully");
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", allMasterDropdown));
    }

    @PostMapping("/add-sub-master")
    public ResponseEntity<CommonResponse> addSubMaster(@RequestBody SubMasterRequestDto dto) {
        log.info("API Call: addSubMaster with request: {}", dto);
        String result = masterService.addSubMaster(dto);

        if ("Sub master already exists.".equals(result)) {
            log.warn("Sub master already exists: {}", dto);
            return ResponseEntity.status(409).body(new CommonResponse(409, result, null));
        }

        log.info("Sub master added successfully: {}", dto);
        return ResponseEntity.ok(new CommonResponse(201, result, null));
    }

    // getALLDropdown
    @GetMapping("/get-all-submaster")
    public ResponseEntity<CommonResponse<Page<DropdownResponseDto>>> getAllSubMaster(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer imMasterId,
            @RequestParam(required = false) String imMasterName,
            @RequestParam(required = false) Integer imSubMasterId,
            @RequestParam(required = false) String imSubMasterName,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate createdOn
    ) {
        log.info("API Call: getAllSubMaster with page: {}, size: {}, imMasterId: {}, imMasterName: {}, imSubMasterId: {}, imSubMasterName: {}, createdBy: {}, createdOn: {}",
                page, size, imMasterId, imMasterName, imSubMasterId, imSubMasterName, createdBy, createdOn);

        Page<DropdownResponseDto> result = masterService.getAllSubMaster(
                page, size, imMasterId, imMasterName, imSubMasterId, imSubMasterName, createdBy, createdOn
        );

        log.info("Fetched all sub masters successfully, count={}", result.getTotalElements());
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", result));
    }


    @GetMapping("/im-sub-master-details")
    public List<ImSubMasterResponseDto> getSubMasterDetails(@RequestParam Integer masterId) {
        log.info("API Call: getSubMasterDetails for masterId: {}", masterId);
        List<ImSubMasterResponseDto> subMasterDetails = masterService.getSubMasterDetails(masterId);
        log.info("Fetched sub master details successfully for masterId: {}", masterId);
        return subMasterDetails;
    }

}
