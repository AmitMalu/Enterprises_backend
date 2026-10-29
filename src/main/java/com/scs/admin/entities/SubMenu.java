package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sub_menus")
@Data
public class SubMenu {

    @Id
    @Column(name = "sub_menu_id",columnDefinition = "CHAR(2)")
    private String subMenuId;

    @Column(name = "menu_id")
    private String menuId;

    @Column(name = "sub_menu_name")
    private String subMenuName;
}
