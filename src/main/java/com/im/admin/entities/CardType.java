package com.im.admin.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "card_type")
@AllArgsConstructor
@NoArgsConstructor
public class CardType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "card_bin_no", unique = true, nullable = false, length = 10)
    private String cardBin_No;

}
