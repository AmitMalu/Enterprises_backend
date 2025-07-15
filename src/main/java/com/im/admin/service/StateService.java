package com.im.admin.service;

import com.im.admin.dto.StateDTO;
import com.im.admin.dto.StateRequestDTO;
import com.im.admin.dto.StateResponseDTO;
import com.im.admin.entities.State;
import com.im.admin.response.CommonResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface StateService {


    List<StateDTO> getStatesByCountry(Long countryId);

    CommonResponse<StateResponseDTO> save(StateRequestDTO dto);

    CommonResponse<StateResponseDTO> getById(Long id);

    Page<StateResponseDTO> getAllStates(int page, int size, String zone, String stateName, String stateCode, String regionType, Long countryId, Boolean blocked);

    CommonResponse<StateResponseDTO> update(Long id, StateRequestDTO dto);

    ByteArrayResource generateSampleTemplate() throws IOException;

    ByteArrayResource saveStatesFromExcel(MultipartFile file, String createdBy);


}
