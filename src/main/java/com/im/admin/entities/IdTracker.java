package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@IdClass(IdTrackerKey.class)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "id_tracker")
public class IdTracker {

    @Id
    @Column(name = "entity_type", nullable = false)
    private String entityType;  // "SuperAdmin", "Admin"

    @Id
    @Column(name = "abbreviation", nullable = false, length = 5)
    private String abbreviation;

    @Column(name = "last_id", nullable = false)
    private int lastId;

}

