package com.scs.admin.repositories;

import com.scs.admin.entities.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu,String> {

    Optional<Menu> findByMenuName(String menuName);
}
