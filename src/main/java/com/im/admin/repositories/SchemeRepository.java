package com.im.admin.repositories;

import com.im.admin.entities.ImBankMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import com.im.admin.entities.Scheme;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long>, JpaSpecificationExecutor<Scheme> {

    boolean existsBySchemeCombinationHashCodeStartingWith(String combinationHash);

    @Query("SELECT COALESCE(MAX(s.id), 0) FROM Scheme s")
    Long findMaxSchemeId();

    boolean existsBySchemeName(String schemeName);

    boolean existsBySchemeCode(String schemeCode);

    Optional<Scheme> findBySchemeCode(String schemeCode);

    @Query(value = "SELECT s.id, s.scheme_name, s.scheme_code " +
            "FROM scheme s " +
            "WHERE SUBSTRING_INDEX(SUBSTRING_INDEX(s.scheme_combination_hash_code, '-', 2), '-', -1) = :subProductId " +
            "AND s.is_active = true",
            nativeQuery = true)
    List<Object[]> findActiveSchemesBySubProductId(@Param("subProductId") String subProductId);

    Optional<Scheme> findBySchemeCodeAndIsActiveTrue(String schemeCode);

    Optional<Scheme> findBySchemeName(String schemeName);

    List<Scheme>
    findBySchemeCodeInAndIsActiveTrue(
            List<String> schemeCodes
    );

}
