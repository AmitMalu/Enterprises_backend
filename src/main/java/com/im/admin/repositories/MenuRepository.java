package com.im.admin.repositories;

import com.im.admin.entities.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu,Integer> {

    Optional<Menu> findByMenuName(String menuName);
}
