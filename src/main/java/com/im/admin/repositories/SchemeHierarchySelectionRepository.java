package com.im.admin.repositories;

import com.im.admin.entities.SchemeHierarchySelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeHierarchySelectionRepository extends JpaRepository<SchemeHierarchySelection, String> {

    List<SchemeHierarchySelection> findBySchemeHierarchyCombinationContainingIgnoreCase(String keyword);

    @Query("SELECT s FROM SchemeHierarchySelection s WHERE s.id = :id")
    Optional<SchemeHierarchySelection> findByCustomId(@Param("id") String id);
}

