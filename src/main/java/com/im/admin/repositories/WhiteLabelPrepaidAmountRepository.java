package com.im.admin.repositories;

import com.im.admin.entities.WhiteLabelPrepaidAmount;
import com.im.admin.entities.WhiteLabels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WhiteLabelPrepaidAmountRepository extends JpaRepository<WhiteLabelPrepaidAmount, Long> {

    void deleteByWhiteLabelId(WhiteLabels whiteLabel);

    Optional<WhiteLabelPrepaidAmount> findByWhiteLabelId_WhiteLabelId(String whiteLabelId);


}