package com.im.admin.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "card_type_master")
@AllArgsConstructor
@NoArgsConstructor
public class CardTypeMaster {
   // ID	Entry	Network	Type	Sub Type	Card iin	Issuer Code	Issuer Name
//	  42171	iin	 MasterCard	credit	consumer	null	unknown	       unknown

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "card_bin_no", unique = true, nullable = false, length = 10)
    private String cardBinNo;     //ID = 42171 like that

    @Column(name = "entry")
    private String entry;   //in

    @Column(name = "network")
    private String network;    //MasterCard, Visa, American Express

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "sub_type")
    private String subType; //consumer, business

    @Column(name = "issuer_code")
    private String issuerCode;

    @Column(name = "issuer_name")
    private String issuerName;

}
