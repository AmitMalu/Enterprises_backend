package com.scs.admin.controller;

/**
 * @author SHUBHAM KHOPADE
 */
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.DigilockerAadhaarResponse;
import com.scs.admin.response.DigilockerInitResponse;
import com.scs.admin.service.DigilockerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/digilocker")
@RequiredArgsConstructor
public class DigilockerController {

    private final DigilockerService digilockerService;

    @PostMapping("/initialize")
    public ResponseEntity<CommonResponse<DigilockerInitResponse>> initializeDigilocker() {

        DigilockerInitResponse response =
                digilockerService.initializeDigilocker();

        return ResponseEntity.ok(
                CommonResponse.success(response, "DigiLocker initialized successfully")
        );
    }

    @GetMapping("/download-aadhaar/{clientId}")
    public ResponseEntity<CommonResponse<DigilockerAadhaarResponse>> downloadAadhaar(
            @PathVariable String clientId) {

        DigilockerAadhaarResponse response =
                digilockerService.downloadAadhaar(clientId);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Aadhaar fetched successfully")
        );
    }
}
