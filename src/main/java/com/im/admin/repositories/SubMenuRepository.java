package com.im.admin.repositories;

import com.im.admin.entities.SubMenu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubMenuRepository extends JpaRepository<SubMenu,String> {
    boolean existsByMenuIdAndSubMenuName(String menuId, String subMenuName);
}
