package com.scs.admin.repositories;

import com.scs.admin.entities.VimoPayStateList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author SHUBHAM KHOPADE
 */
@Repository
public interface VimoPayStateListRepository extends JpaRepository<VimoPayStateList, Long> {

    Optional<VimoPayStateList> findByVendor_IdAndStateName(
            String vendorId,
            String stateName
    );

    Optional<VimoPayStateList> findByStateNameIgnoreCase(
            String stateName
    );

}
