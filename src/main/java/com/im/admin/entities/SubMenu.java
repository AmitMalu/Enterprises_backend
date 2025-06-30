package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sub_menus")
@Data
public class SubMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sub_menu_id")
    private Integer subMenuId;

    @Column(name = "menu_id")
    private Integer menuId;

    @Column(name = "sub_menu_name")
    private String subMenuName;
}
