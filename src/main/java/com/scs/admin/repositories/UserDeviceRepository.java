package com.scs.admin.repositories;

import com.scs.admin.entities.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserIdAndDeviceId(String userId, String deviceId);

    Optional<UserDevice> findByDeviceId(String deviceId);

    List<UserDevice> findByUserId(String userId);
}
