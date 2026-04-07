package com.im.admin.repositories;

import com.im.admin.entities.ProductTransactionLimit;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.product.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductTransactionLimitRepository extends JpaRepository<ProductTransactionLimit, Long> {

    Optional<ProductTransactionLimit> findByProduct_Id(Short productId);

    Optional<ProductTransactionLimit> findByProductAndSubProduct(Products product, SubProducts subProduct);

    Optional<ProductTransactionLimit> findByProduct_IdAndSubProduct_Id(Short productId, String subProductId);

}
