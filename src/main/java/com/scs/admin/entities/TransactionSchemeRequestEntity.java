package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_scheme_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSchemeRequestEntity {

    @Id
    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Lob
    @Column(name = "full_request_dto", columnDefinition = "TEXT", nullable = false)
    private String fullRequestDtoJson;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

}

