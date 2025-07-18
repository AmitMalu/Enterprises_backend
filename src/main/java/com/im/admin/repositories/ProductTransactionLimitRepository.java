package com.im.admin.repositories;

import com.im.admin.entities.ProductTransactionLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductTransactionLimitRepository extends JpaRepository<ProductTransactionLimit, Long> {

    Optional<ProductTransactionLimit> findByProduct_Id(Short productId);


}
