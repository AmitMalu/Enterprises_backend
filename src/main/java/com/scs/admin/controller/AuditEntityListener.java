package com.scs.admin.controller;

import com.scs.admin.entities.BaseEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditEntityListener {

    @PrePersist
    public void setCreatedBy(Object entity) {
        if (entity instanceof BaseEntity) {
            String username = getAuthenticatedUsername();
            ((BaseEntity) entity).setCreatedBy(username);
            ((BaseEntity) entity).setEditedBy(username);
        }
    }

    @PreUpdate
    public void setEditedBy(Object entity) {
        if (entity instanceof BaseEntity) {
            String username = getAuthenticatedUsername();
            ((BaseEntity) entity).setEditedBy(username);
        }
    }

    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "SYSTEM";
    }
}

