package com.im.admin.controller;


import com.im.admin.dto.ImSubMasterResponseDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.MasterService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/im-master-names")
    public List<String> getAllMasterNames() {
        return masterService.getAllMasterNames();

    }

    @GetMapping("/im-sub-master-details")
    public List<ImSubMasterResponseDto> getSubMasterDetails(@RequestParam String masterId) {
        List<ImSubMasterResponseDto> subMasterDetails = masterService.getSubMasterDetails(masterId);
        return subMasterDetails;
    }

}
