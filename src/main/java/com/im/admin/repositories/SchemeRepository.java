package com.im.admin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.im.admin.entities.Scheme;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long> {

    boolean existsBySchemeCombinationHashCodeStartingWith(String combinationHash);

    @Query("SELECT COALESCE(MAX(s.id), 0) FROM Scheme s")
    Long findMaxSchemeId();

    boolean existsBySchemeName(String schemeName);

    boolean existsBySchemeCode(String schemeCode);

    Optional<Scheme> findBySchemeCode(String schemeCode);

}
