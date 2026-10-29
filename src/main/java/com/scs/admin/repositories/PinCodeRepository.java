package com.scs.admin.repositories;

import com.scs.admin.entities.PinCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PinCodeRepository extends JpaRepository<PinCode, Long>, JpaSpecificationExecutor<PinCode> {

    List<PinCode> findByCity_Id(Long cityId);

    Optional<PinCode> findByPinCode(String pinCode);

    boolean existsByPinCode(String pinCode);

    boolean existsByPinCodeAndIdNot(String pinCode, Long id);

    Optional<PinCode>
    findByPinCodeAndCityId(
            String pin,
            Long cityId
    );

    @Query("""
            SELECT p
            FROM PinCode p
            JOIN FETCH p.city
            """)
    List<PinCode> findAllPins();

}

