package com.im.admin.repositories;

import com.im.admin.entities.SchemeHierarchySelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemeHierarchySelectionRepository extends JpaRepository<SchemeHierarchySelection, Long> {

    List<SchemeHierarchySelection> findByCombinationHashContainingIgnoreCase(String keyword);
}

