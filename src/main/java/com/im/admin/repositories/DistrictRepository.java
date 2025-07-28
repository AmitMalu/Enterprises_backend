package com.im.admin.repositories;

import com.im.admin.entities.District;
import com.im.admin.entities.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long>, JpaSpecificationExecutor<District> {

    List<District> findByStateId(Long stateId);

    boolean existsByDistrictCode(String districtCode);

    Optional<District> findByDistrictNameIgnoreCaseAndState(String districtName, State state);


}

