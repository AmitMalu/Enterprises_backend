package com.im.admin.repositories;

import com.im.admin.entities.product.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductsRepository extends JpaRepository<Products, Short> {

    @Query("SELECT MAX(p.id) FROM Products p")
    Short findMaxId();

    //@Query("SELECT COALESCE(MAX(p.id), 9) FROM Products p")
    //Short findMaxProductId();


}

