package com.im.admin.repositories;

import com.im.admin.dto.DropdownResponseDto;
import com.im.admin.entities.ImSubMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ImSubMasterRepository extends JpaRepository<ImSubMaster, Integer> {

    List<ImSubMaster> findByImMasterId(Integer masterId);

    boolean existsByImMasterIdAndImSubMasterName(Integer imMasterId, String imSubMasterName);

    @Query(
            value = "SELECT m.im_master_id, m.im_master_name, s.im_sub_master_id, s.im_sub_master_name " +
                    "FROM im_master m JOIN im_sub_master s ON m.im_master_id = s.im_master_id",
            countQuery = "SELECT COUNT(*) FROM im_master m JOIN im_sub_master s ON m.im_master_id = s.im_master_id",
            nativeQuery = true
    )
    Page<Object[]> getAllDropdowns(Pageable pageable);


}
