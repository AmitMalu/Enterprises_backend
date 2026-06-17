package com.im.admin.controller;

import com.im.admin.response.CommonResponse;
import com.im.admin.service.impl.LocationUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author SHUBHAM KHOPADE
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/master")
public class LocationController {

    private final LocationUploadService service;

    /**
     * Upload a .xlsx / .xls / .csv file with location data.
     *
     * Expected column order:
     *   0: Pincode  1: City  2: District  3: State  4: Country
     *   5: CountryCode (optional)  6: StateCode (optional)
     *   7: RegionType (optional)   8: DistrictCode (optional)
     *   9: CityCode (optional)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<String>> upload(@RequestParam MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    CommonResponse.handleErrors("File is empty"));
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null ||
                !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls") || fileName.endsWith(".csv"))) {
            return ResponseEntity.badRequest().body(
                    CommonResponse.handleErrors("Only .xlsx / .xls / .csv files are supported"));
        }

        log.info("Location upload triggered — file={}", fileName);
        service.upload(file);

        return ResponseEntity.ok(
                CommonResponse.success("Upload completed successfully", "Location data imported"));
    }
}
