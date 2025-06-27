package com.im.admin.controller;


import com.im.admin.dto.*;
import com.im.admin.entities.ImMaster;
import com.im.admin.entities.ImSubMaster;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.MasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/master")
public class MasterController {

    @Autowired
    private MasterService masterService;

    @PostMapping("/card-type")
    public ResponseEntity<CommonResponse<?>> cardType() {
        return null;
    }


    @PostMapping
    public ResponseEntity<CommonResponse> addMaster(@RequestBody MasterRequestDto masterRequestDto) {
        String result = masterService.addMaster(masterRequestDto);

        if ("Master already exists.".equals(result)) {
            return ResponseEntity
                    .status(409) // Conflict
                    .body(new CommonResponse(409, result, null));
        }
        return ResponseEntity
                .ok(new CommonResponse(201, result, null));
    }


    @GetMapping
    public ResponseEntity<CommonResponse<Page<MasterResponseDto>>> getAllMasters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer imMasterId,
            @RequestParam(required = false) String imMasterName
    ) {
        Page<MasterResponseDto> result = masterService.getAllMasters(page, size, imMasterId, imMasterName);
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<MasterResponseDto>> getMasterById(@PathVariable Integer id) {
        MasterResponseDto dto = masterService.getMasterById(id);
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", dto));
    }

    @PostMapping("/add-sub-master")
    public ResponseEntity<CommonResponse> addSubMaster(@RequestBody SubMasterRequestDto dto) {
        String result = masterService.addSubMaster(dto);

        if ("Sub master already exists.".equals(result)) {
            return ResponseEntity.status(409).body(new CommonResponse(409, result, null));
        }

        return ResponseEntity.ok(new CommonResponse(201, result, null));
    }

    //getALLDropdown
    @GetMapping("/get-all-submaster")
    public ResponseEntity<CommonResponse<Page<DropdownResponseDto>>> getAllSubMaster(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer imMasterId,
            @RequestParam(required = false) String imMasterName,
            @RequestParam(required = false) Integer imSubMasterId,
            @RequestParam(required = false) String imSubMasterName
    ) {
        Page<DropdownResponseDto> result = masterService.getAllSubMaster(
                page, size, imMasterId, imMasterName, imSubMasterId, imSubMasterName);

        return ResponseEntity.ok(new CommonResponse<>(200, "Success", result));
    }

    @GetMapping("/im-sub-master-details")
    public List<ImSubMasterResponseDto> getSubMasterDetails(@RequestParam Integer  masterId) {
        List<ImSubMasterResponseDto> subMasterDetails = masterService.getSubMasterDetails(masterId);
        return subMasterDetails;
    }


}
