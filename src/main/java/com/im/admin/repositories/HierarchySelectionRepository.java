package com.im.admin.repositories;


import com.im.admin.entities.SchemeHierarchySelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface HierarchySelectionRepository extends JpaRepository<SchemeHierarchySelection, String> {

    boolean existsByCombinationHash(String combinationHash);

    @Query("SELECT MAX(s.id) FROM SchemeHierarchySelection s")
    String findMaxId();

}

