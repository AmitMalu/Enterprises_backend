package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
@Entity
@Table(
        name = "webhook_idempotency",
        uniqueConstraints = @UniqueConstraint(name = "uk_webhook_idempotency_id", columnNames = "idempotency_id")
)
public class WebhookIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CASHFREE cf_payment_id
    @Column(name = "idempotency_id", nullable = false, length = 100)
    private String idempotencyId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
