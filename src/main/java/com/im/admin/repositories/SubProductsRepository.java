package com.im.admin.repositories;

import com.im.admin.entities.SubProducts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubProductsRepository extends JpaRepository<SubProducts, Long> {

}
