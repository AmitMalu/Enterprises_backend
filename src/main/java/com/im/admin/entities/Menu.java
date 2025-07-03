package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "menus")
@Data
public class Menu {

    @Id
    @Column(name = "menu_id", columnDefinition = "CHAR(2)")
    private String menuId;

    @Column(name = "menu_name")
    private String menuName;
}
