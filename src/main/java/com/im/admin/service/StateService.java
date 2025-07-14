package com.im.admin.service;

import com.im.admin.entities.State;
import org.springframework.data.domain.Page;

public interface StateService {

    State createState(State state);

    State updateState(Long id, State state);

    State getStateById(Long id);

    Page<State> getFilteredStates(int page, int size,
                                  String zone, String stateName, String stateCode,
                                  String regionType, Boolean blocked, String createdBy, String editedBy);
}
