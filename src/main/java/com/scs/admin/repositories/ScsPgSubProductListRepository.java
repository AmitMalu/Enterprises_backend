package com.scs.admin.repositories;

import com.scs.admin.entities.ScsPgSubProductList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author SHUBHAM KHOPADE
 */
@Repository
public interface ScsPgSubProductListRepository extends JpaRepository<ScsPgSubProductList, Long> {

    Optional<ScsPgSubProductList> findByExcelId(String excelId);

    List<ScsPgSubProductList> findAllByExcelIdIn(
            Collection<String> excelIds
    );

    boolean existsByExcelId(String excelId);

    List<ScsPgSubProductList> findByProductIgnoreCaseAndSettlementCategoryIgnoreCase(String product, String settlementCategory);
}
