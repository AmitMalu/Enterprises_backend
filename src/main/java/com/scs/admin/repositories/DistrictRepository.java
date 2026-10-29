package com.scs.admin.repositories;

import com.scs.admin.entities.District;
import com.scs.admin.entities.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long>, JpaSpecificationExecutor<District> {

    List<District> findByStateId(Long stateId);

    boolean existsByDistrictCode(String districtCode);

    Optional<District> findByDistrictNameIgnoreCaseAndState(String districtName, State state);

    Optional<District>
    findByDistrictNameIgnoreCaseAndStateId(
            String district,
            Long stateId
    );

    @Query("""
            SELECT d
            FROM District d
            JOIN FETCH d.state
            """)
    List<District> findAllDistricts();

}

