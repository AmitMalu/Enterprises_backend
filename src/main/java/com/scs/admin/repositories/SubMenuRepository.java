package com.scs.admin.repositories;

import com.scs.admin.entities.SubMenu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubMenuRepository extends JpaRepository<SubMenu,String> {
    boolean existsByMenuIdAndSubMenuName(String menuId, String subMenuName);
}
