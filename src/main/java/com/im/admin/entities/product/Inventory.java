package com.im.admin.entities.product;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;


@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class Inventory {

    @NotNull(message = "Inventory status is required")
    @Column(name = "is_inventory", nullable = false)
    private Boolean isInventory;

    @NotNull(message = "Inventory price is required")
    @Column(name = "inventory_price", nullable = false)
    private Double inventoryPrice;

    @NotNull(message = "Virtual status is required")
    @Column(name = "is_virtual", nullable = false)
    private Boolean isVirtual;

    @Column(name = "is_show")
    private Boolean isShow;
}
