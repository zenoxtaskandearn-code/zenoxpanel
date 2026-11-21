package com.gallery.platform.admin;

import com.gallery.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "admins")
public class Admin extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role = "ADMIN";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
