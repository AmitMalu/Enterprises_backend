package com.im.admin.repositories;


import com.im.admin.entities.SchemeHierarchySelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface HierarchySelectionRepository extends JpaRepository<SchemeHierarchySelection, Long> {

    boolean existsByCombinationHash(String combinationHash);

}

