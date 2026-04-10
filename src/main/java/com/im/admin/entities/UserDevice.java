package com.im.admin.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_devices")
@Getter
@Setter
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;   // hierarchyId (from UserDetails)

    private String deviceId; // unique device identifier from mobile app

    private String mpin;     // hashed MPIN (bcrypt)

    private Boolean isTrusted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(name = "mpin_attempts")
    private Integer mpinAttempts = 0;

    @Column(name = "mpin_locked")
    private Boolean mpinLocked = false;
}
