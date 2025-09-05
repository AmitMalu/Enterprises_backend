package com.im.admin.repositories;

import com.im.admin.entities.Admin;
import com.im.admin.entities.WhiteLabels;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhiteLabelRepository extends JpaRepository<WhiteLabels, String> {

    boolean existsByAbbreviation(String abbreviation);

    List<WhiteLabels> findByStatus(String status);

    List<WhiteLabels> findByParentAccountContaining(String parentType);

    List<WhiteLabels> findByStatusIgnoreCase(String status);

    Optional<WhiteLabels> findByWhiteLabelId(String whiteLabelId);

    List<WhiteLabels> findByIsAdminTrue();

    WhiteLabels findByMobileNumber(@NotBlank(message = "Mobile number is required") @Pattern(regexp = "\\d{10}", message = "Mobile number must be 10 digits") String mobileNumber);

    @Query("SELECT w.name FROM WhiteLabels w WHERE w.whiteLabelId = :hierarchyId")
    String findNameByHierarchyId(@Param("hierarchyId") String hierarchyId);

    List<WhiteLabels> findByParentAccount(String fromId);

    List<WhiteLabels> findByParentAccountContainingIgnoreCase(String fromId);



}


