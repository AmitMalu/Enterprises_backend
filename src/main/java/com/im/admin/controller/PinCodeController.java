package com.im.admin.controller;

import com.im.admin.dto.PinCodeDTO;
import com.im.admin.dto.PinCodeRequestDTO;
import com.im.admin.dto.PinCodeResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PinCodeLocationResponse;
import com.im.admin.service.PinCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pincodes")
@RequiredArgsConstructor
public class PinCodeController {

    @Autowired
    private PinCodeService pinCodeService;

    @PostMapping
    public ResponseEntity<CommonResponse<PinCodeResponseDTO>> create(@RequestBody PinCodeRequestDTO dto) {
        return ResponseEntity.ok(pinCodeService.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<PinCodeResponseDTO>> update(@PathVariable Long id, @RequestBody PinCodeRequestDTO dto) {
        return ResponseEntity.ok(pinCodeService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<PinCodeResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(pinCodeService.getById(id));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<PinCodeResponseDTO>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String pinCode,
            @RequestParam(required = false) Boolean blocked,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy
    ) {
        return ResponseEntity.ok(
                pinCodeService.getAll(page, size, pinCode, blocked, createdBy, editedBy)
        );
    }

    @GetMapping("/by-city/{cityId}")
    public ResponseEntity<CommonResponse<List<PinCodeDTO>>> getByCity(@PathVariable Long cityId) {
        return ResponseEntity.ok(CommonResponse.success(
                pinCodeService.getPinCodesByCity(cityId), "Pin codes loaded successfully"));
    }

    @GetMapping("/lookup/{pinCode}")
    public ResponseEntity<CommonResponse<PinCodeLocationResponse>> lookupLocation(@PathVariable String pinCode) {
        PinCodeLocationResponse response = pinCodeService.lookupByPinCode(pinCode);
        return ResponseEntity.ok(CommonResponse.success(response, "Location found for given pin code"));
    }

}

