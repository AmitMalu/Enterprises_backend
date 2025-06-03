package com.im.admin.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ticket_comment")
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    @JsonIgnore
    private Ticket ticket;

    @Column(name = "comment_text",columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "added_by")
    private String addedBy; // "USER" or "ADMIN"

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
