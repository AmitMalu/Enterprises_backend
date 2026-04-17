package com.im.admin.repositories;

import com.im.admin.entities.NotificationMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationMasterRepository extends JpaRepository<NotificationMaster, Long> {
    Page<NotificationMaster> findByAudienceTypeInIgnoreCase(
            List<String> audienceTypes,
            Pageable pageable
    );

    List<NotificationMaster> findByAudienceTypeInIgnoreCase(
            List<String> audienceTypes
    );

    Page<NotificationMaster> findByAudienceTypeInIgnoreCaseAndDeliveryChannelInIgnoreCase(
            List<String> audienceTypes,
            List<String> deliveryChannels,
            Pageable pageable
    );

}
