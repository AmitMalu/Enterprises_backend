package com.im.admin.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "id_tracker")
public class IdTracker {

    @Id
    @Column(name = "entity_type", nullable = false)
    private String entityType;  // "SuperAdmin", "Admin"

    @Column(name = "last_id", nullable = false)
    private int lastId;

}

