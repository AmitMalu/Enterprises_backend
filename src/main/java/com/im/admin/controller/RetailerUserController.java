package com.im.admin.controller;

import com.im.admin.entities.RetailerUser;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.RetailerUserResponseDTO;
import com.im.admin.service.RetailerUserService;
import com.im.admin.spec.projectionFilter.RetailerUserProjection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/retailer")
public class RetailerUserController {

    @Autowired
    private RetailerUserService retailerUserService;

    @PostMapping("/create-user")
    public ResponseEntity<CommonResponse<RetailerUser>> createAgent(@RequestBody RetailerUser retailerUser) {
        log.info("Received request to create Retailer Agent: {}", retailerUser);

        RetailerUser savedAgent = retailerUserService.createRetailerUser(retailerUser);

        CommonResponse<RetailerUser> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.CREATED.value());
        response.setMessage("Retailer User created successfully");
        response.setData(savedAgent);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/get-all/filter")
    public ResponseEntity<CommonResponse<Map<String, Object>>> getFilteredUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String emailId,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) Boolean status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(
                retailerUserService.getFilteredUsers(
                        name, mobileNumber, emailId, emailVerified, status,
                        page, size, sortBy, sortDir
                )
        );
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<CommonResponse<Void>> deleteRetailerUser(@PathVariable String id) {
        return ResponseEntity.ok(retailerUserService.deleteRetailerUser(id));
    }

    @PutMapping("/update-user/{id}")
    public ResponseEntity<CommonResponse<RetailerUser>> updateAgent(
            @PathVariable String id,
            @RequestBody RetailerUser retailerUser) {

        RetailerUser updatedAgent = retailerUserService.updateRetailerUser(id, retailerUser);

        CommonResponse<RetailerUser> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Retailer User updated successfully");
        response.setData(updatedAgent);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{retailerId}")
    public ResponseEntity<CommonResponse<Page<RetailerUserResponseDTO>>> getRetailerUsersByRetailerId(
            @PathVariable String retailerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String retailerUserId,
            @RequestParam(required = false) String abbreviation,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String emailId,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy,
            @RequestParam(required = false) String role_id,
            @RequestParam(required = false) String subRoleId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate createdOn
    ) {
        log.info("Fetching Retailer Users for Retailer ID: {} (page: {}, size: {})", retailerId, page, size);

        CommonResponse<Page<RetailerUserResponseDTO>> response = retailerUserService.getRetailerUsersByRetailerId(
                retailerId, retailerUserId, abbreviation, name, mobileNumber, username, emailId,
                emailVerified, status, createdBy, editedBy, role_id, subRoleId, createdOn, page, size
        );

        return ResponseEntity.ok(response);
    }




}

