package com.im.admin.controller;

import com.im.admin.dto.BankListRequest;
import com.im.admin.dto.BankRequestDto;
import com.im.admin.entities.NetworkBankMaster;
import com.im.admin.request.NetworkBankMasterResponseDto;
import com.im.admin.response.BankInfoResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.NetworkBankDropdownResponse;
import com.im.admin.service.NetworkBankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequestMapping("/api/network-banks")
public class NetworkBankController {

    @Autowired
    private NetworkBankService bankService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<NetworkBankMaster>> addBank(
            @RequestPart("bankData") BankRequestDto bankRequestDto,
            @RequestPart(value = "logo", required = false) MultipartFile logoFile) {

        log.info("Received request to add new BankMaster: {}", bankRequestDto);

        try {
            NetworkBankMaster savedBank = bankService.addBank(bankRequestDto, logoFile);
            CommonResponse<NetworkBankMaster> response = new CommonResponse<>(201, "BankMaster created successfully", savedBank);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException ex) {
            log.error("Invalid request while adding BankMaster: {}", ex.getMessage());
            CommonResponse<NetworkBankMaster> response = new CommonResponse<>(400, ex.getMessage(), null);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            log.error("Logo upload failed: {}", e.getMessage());
            return new ResponseEntity<>(new CommonResponse<>(500, "Logo upload failed", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<NetworkBankMaster>>> getAllBanks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,

            @RequestParam(required = false) String id,
            @RequestParam(required = false) String bankCode,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) String bankBranchName,
            @RequestParam(required = false) String ifscCode,
            @RequestParam(required = false) Boolean imps,
            @RequestParam(required = false) Boolean neft,
            @RequestParam(required = false) Boolean rtgs,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy,
            @RequestParam(required = false) Boolean isBlocked,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate createdOn,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate editedOn
    ) {
        log.info("Fetching BankMaster list with filters - bankName: {}, bankCode: {}", bankName, bankCode);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdOn"));
        Page<NetworkBankMaster> banks = bankService.getAllBanks(
                pageable, id, bankCode, bankName, bankBranchName, ifscCode,
                imps, neft, rtgs, isActive,
                createdBy, editedBy, isBlocked, createdOn, editedOn
        );

        CommonResponse<Page<NetworkBankMaster>> response = new CommonResponse<>(
                200,
                "BankMaster fetched successfully",
                banks
        );
        return ResponseEntity.ok(response);
    }


    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<NetworkBankMaster>> updateBank(
            @PathVariable Long id,
            @RequestPart("bankData") BankRequestDto bankRequestDto,
            @RequestPart(value = "logo", required = false) MultipartFile logoFile) {

        log.info("Received request to update BankMaster with ID: {}. Data: {}", id, bankRequestDto);
        NetworkBankMaster updatedBank = bankService.updateBank(id, bankRequestDto, logoFile);

        return ResponseEntity.ok(new CommonResponse<>(200, "BankMaster updated successfully", updatedBank));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<NetworkBankMaster>> getBankById(@PathVariable Long id) {
        log.info("Received request to fetch bank with ID: {}", id);

        NetworkBankMaster bank = bankService.getBankById(id);
        CommonResponse<NetworkBankMaster> response = new CommonResponse<>(
                200, "Bank fetched successfully", bank
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dropdown")
    public ResponseEntity<CommonResponse<List<NetworkBankDropdownResponse>>> getBankDropdown() {
        log.info("Fetching bank dropdown list");
        List<NetworkBankDropdownResponse> list = bankService.getBankDropdownList();
        return ResponseEntity.ok(CommonResponse.success(list, "Bank dropdown data loaded successfully"));
    }

    @GetMapping("/details-by-ifsc/{ifscCode}")
    public ResponseEntity<CommonResponse<BankInfoResponse>> getBankDetailsByIfsc(@PathVariable String ifscCode) {
        log.info("Fetching bank details by IFSC code: {}", ifscCode);
        try {
            BankInfoResponse response = bankService.getBankInfoByIfsc(ifscCode);
            return ResponseEntity.ok(CommonResponse.success(response, "Bank details fetched successfully"));
        } catch (NoSuchElementException ex) {
            log.error("Bank details not found for IFSC: {}", ifscCode);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.failure(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Error fetching bank details for IFSC {}: {}", ifscCode, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.failure("Something went wrong"));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<CommonResponse<List<NetworkBankMasterResponseDto>>>
    getAllNetworkBanks() {

        List<NetworkBankMasterResponseDto> banks =
                bankService.getAllNetworkBanks();

        if (banks.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.failure("Network bank list not found"));
        }

        return ResponseEntity.ok(
                CommonResponse.success(banks,
                        "Network bank list fetched successfully")
        );
    }

    @PostMapping("/save")
    public ResponseEntity<String> saveBankList(@RequestBody BankListRequest request) {

        bankService.saveBankList(request);

        return ResponseEntity.ok("Bank list saved successfully");

    }

    // ─────────────────────────────────────────────────────────────────────────
    // BULK UPLOAD
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bulk upload bank master records from an .xlsx / .xls file.
     *
     * Expected column order (row 0 = header, data starts at row 1):
     *   0  Bank Code       1  Bank Code BI     2  Bank Name (mandatory)
     *   3  Vidual Bank Code 4  IMPS            5  NEFT
     *   6  Logo            7  IFSC Code        8  Max Account No Length
     *   9  Verification    10 Active Status    11 Blocked
     *
     * IMPS / NEFT / Verification / Blocked: Y or N (case-insensitive)
     * Active Status: S (active) or H (hold/inactive)
     *
     * Duplicate bank names (case-insensitive) are skipped automatically.
     * Each batch of 500 rows is committed independently — no giant rollback.
     */
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<com.im.admin.service.BankBulkUploadResult>> bulkUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "createdBy", required = false, defaultValue = "SYSTEM") String createdBy) {

        log.info("BankMaster bulk upload request — file={} createdBy={}",
                file.getOriginalFilename(), createdBy);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(400, "File is empty", null));
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(400, "Only .xlsx / .xls files are supported", null));
        }

        com.im.admin.service.BankBulkUploadResult result =
                bankService.bulkUpload(file, createdBy);

        String message = String.format(
                "Upload complete — %d inserted, %d skipped, %d errors in %dms",
                result.getInserted(), result.getSkipped(),
                result.getErrorCount(), result.getElapsedMs());

        return ResponseEntity.ok(new CommonResponse<>(200, message, result));
    }

}
