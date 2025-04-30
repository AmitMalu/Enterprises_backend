package com.im.admin.repositories;

import com.im.admin.entities.product.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductsRepository extends JpaRepository<Products, Short> {

    @Query("SELECT MAX(p.id) FROM Products p")
    Short findMaxId();

    List<Products> findByActiveTrue();

    Optional<Products> findByProductCode(String productCode);

}

