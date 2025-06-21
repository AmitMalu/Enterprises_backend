package com.im.admin.controller;


import com.im.admin.dto.DropdownRequestDto;
import com.im.admin.dto.DropdownResponseDto;
import com.im.admin.dto.ImSubMasterResponseDto;
import com.im.admin.dto.ProductDropdownResponseDto;
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

    @GetMapping("/get-all-dropdown")
    public ResponseEntity<CommonResponse<Page<DropdownResponseDto>>> getAllDropdowns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<DropdownResponseDto> result = masterService.getAllDropdowns(page, size);

        return ResponseEntity.ok(
                new CommonResponse<>(200, "Success", result));
    }




    @GetMapping("/im-sub-master-details")
    public List<ImSubMasterResponseDto> getSubMasterDetails(@RequestParam Integer  masterId) {
        List<ImSubMasterResponseDto> subMasterDetails = masterService.getSubMasterDetails(masterId);
        return subMasterDetails;
    }


    @PostMapping("/add-dropdown")
    public ResponseEntity<CommonResponse> addDropdown(@RequestBody DropdownRequestDto dto) {
        String result = masterService.addDropdown(dto);

        if ("Sub master already exists.".equals(result)) {
            return ResponseEntity
                    .status(409) // Conflict
                    .body(new CommonResponse(409, result, null));
        }
        return ResponseEntity
                .ok(new CommonResponse(200, result, null));

    }

}
