package com.im.admin.repositories;

import com.im.admin.entities.UserNotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserNotificationReadRepository extends JpaRepository<UserNotificationRead, Long> {

    boolean existsByUserIdAndNotificationId(String userId, Long notificationId);

    List<UserNotificationRead> findByUserId(String userId);
}
