package com.im.admin.repositories;

import com.im.admin.entities.ImPgSubProductList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author SHUBHAM KHOPADE
 */
@Repository
public interface ImPgSubProductListRepository extends JpaRepository<ImPgSubProductList, Long> {

    Optional<ImPgSubProductList> findByExcelId(String excelId);

    List<ImPgSubProductList> findAllByExcelIdIn(
            Collection<String> excelIds
    );

    boolean existsByExcelId(String excelId);
}
