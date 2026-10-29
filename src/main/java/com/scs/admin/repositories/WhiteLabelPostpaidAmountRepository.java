package com.scs.admin.repositories;

import com.scs.admin.entities.WhiteLabelPostpaidAmount;
import com.scs.admin.entities.WhiteLabelPrepaidAmount;
import com.scs.admin.entities.WhiteLabels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WhiteLabelPostpaidAmountRepository extends JpaRepository<WhiteLabelPostpaidAmount, Long> {

    void deleteByWhiteLabelId(WhiteLabels whiteLabel);

    Optional<WhiteLabelPostpaidAmount> findByWhiteLabelId_WhiteLabelId(String whiteLabelId);


}

