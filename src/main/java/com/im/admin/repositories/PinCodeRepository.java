package com.im.admin.repositories;

import com.im.admin.entities.PinCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PinCodeRepository extends JpaRepository<PinCode, Long>, JpaSpecificationExecutor<PinCode> {

    List<PinCode> findByCity_Id(Long cityId);

    Optional<PinCode> findByPinCode(String pinCode);

    boolean existsByPinCode(String pinCode);

    boolean existsByPinCodeAndIdNot(String pinCode, Long id);

}

