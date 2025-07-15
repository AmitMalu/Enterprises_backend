package com.im.admin.controller;

import com.im.admin.dto.StateDTO;
import com.im.admin.entities.State;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.StateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/state")
public class StateController {

    @Autowired
    private StateService stateService;

    @PostMapping
    public ResponseEntity<CommonResponse<State>> create(@RequestBody State state) {
        State saved = stateService.createState(state);
        return ResponseEntity.ok(new CommonResponse<>(201, "State added", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<State>> update(@PathVariable Long id, @RequestBody State state) {
        State updated = stateService.updateState(id, state);
        return ResponseEntity.ok(new CommonResponse<>(200, "State updated", updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<State>> getById(@PathVariable Long id) {
        State state = stateService.getStateById(id);
        return ResponseEntity.ok(new CommonResponse<>(200, "State found", state));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<State>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) String stateCode,
            @RequestParam(required = false) String regionType,
            @RequestParam(required = false) Boolean blocked,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy
    ) {
        Page<State> result = stateService.getFilteredStates(
                page, size, zone, stateName, stateCode, regionType, blocked, createdBy, editedBy
        );
        return ResponseEntity.ok(new CommonResponse<>(200, "States fetched", result));
    }

    @GetMapping("/by-country/{countryId}")
    public ResponseEntity<CommonResponse<List<StateDTO>>> getStatesByCountry(@PathVariable Long countryId) {
        List<StateDTO> states = stateService.getStatesByCountry(countryId);
        return ResponseEntity.ok(CommonResponse.success(states, "States loaded successfully"));
    }

}
