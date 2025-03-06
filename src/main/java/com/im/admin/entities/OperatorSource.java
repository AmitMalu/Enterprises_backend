package com.im.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "operator_sources")
public class OperatorSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(name = "title")
    private String title;

    @NotBlank(message = "Value is required")
    @Column(name = "value")
    private String value;

    @Column(name = "operatorClass")
    private String operatorClass;

    @NotNull(message = "Blocked status is required")
    @Column(name = "blocked")
    private Boolean blocked;

    private Integer sort;

    private String role;
}

