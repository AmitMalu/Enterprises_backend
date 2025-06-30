package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "menus")
@Data
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Integer menuId;

    @Column(name = "menu_name")
    private String menuName;
}
