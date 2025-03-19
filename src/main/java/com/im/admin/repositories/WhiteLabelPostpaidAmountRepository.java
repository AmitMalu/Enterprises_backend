package com.im.admin.repositories;

import com.im.admin.entities.WhiteLabelPostpaidAmount;
import com.im.admin.entities.WhiteLabels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhiteLabelPostpaidAmountRepository extends JpaRepository<WhiteLabelPostpaidAmount, Long> {

    void deleteByWhiteLabelId(WhiteLabels whiteLabel);

}

