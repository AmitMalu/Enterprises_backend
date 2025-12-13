package com.im.admin.repositories;

import com.im.admin.entities.BillPayConfig;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BillPayConfigRepository extends JpaRepository<BillPayConfig,Long> {

    @Query("SELECT DISTINCT v.serviceName FROM BillPayConfig v WHERE v.vendor = :vendor")
    List<String> findDistinctByVendor(String vendor);


    @Query(value = "SELECT provider_id, provider_name FROM billpay_config " +
            "WHERE vendor = :vendor AND service_name = :serviceName",
            nativeQuery = true)
    List<Object[]> findProviders(
            @Param("vendor") String vendor,
            @Param("serviceName") String serviceName
    );

    @Query(value = "SELECT raw_config FROM billpay_config " +
            "WHERE vendor = :vendor AND provider_id = :providerId",
            nativeQuery = true)
    String findRawConfig(String vendor, String providerId);


    Optional<BillPayConfig> findByProviderId(String requestedBillerId);
}
