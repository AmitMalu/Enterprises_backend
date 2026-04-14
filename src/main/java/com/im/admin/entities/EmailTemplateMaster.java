package com.im.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "email_template_master")
@Getter
@Setter
public class EmailTemplateMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template", columnDefinition = "TEXT")
    private String template;

    @Column(name = "type")
    private String type;
}
