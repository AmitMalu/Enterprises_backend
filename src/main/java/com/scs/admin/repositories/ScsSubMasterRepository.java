package com.scs.admin.repositories;

import com.scs.admin.dto.DropdownResponseDto;
import com.scs.admin.entities.ScsSubMaster;
import com.scs.admin.entities.product.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScsSubMasterRepository extends JpaRepository<ScsSubMaster, Integer>, JpaSpecificationExecutor<ScsSubMaster>  {

    List<ScsSubMaster> findByScsMasterId(Integer masterId);

    boolean existsByScsMasterIdAndScsSubMasterName(Integer scsMasterId, String scsSubMasterName);

    @Query(
            value = "SELECT m.scs_master_id, m.scs_master_name, " +
                    "s.scs_sub_master_id, s.scs_sub_master_name, " +
                    "s.created_by, s.created_on " +
                    "FROM scs_master m " +
                    "JOIN scs_sub_master s ON m.scs_master_id = s.scs_master_id " +
                    "ORDER BY m.scs_master_id, s.scs_sub_master_id",
            countQuery = "SELECT COUNT(*) FROM scs_master m JOIN scs_sub_master s ON m.scs_master_id = s.scs_master_id",
            nativeQuery = true
    )
    Page<Object[]> getAllDropdowns(Pageable pageable);


}
