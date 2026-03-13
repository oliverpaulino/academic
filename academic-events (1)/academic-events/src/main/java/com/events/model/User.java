package com.events.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean blocked = false;

    @Column(nullable = false)
    private boolean deletable = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public User() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String v) {
        this.username = v;
    }

    public void setEmail(String v) {
        this.email = v;
    }

    public void setPassword(String v) {
        this.password = v;
    }

    public void setRole(Role v) {
        this.role = v;
    }

    public void setBlocked(boolean v) {
        this.blocked = v;
    }

    public void setDeletable(boolean v) {
        this.deletable = v;
    }

    public void setCreatedAt(LocalDateTime v) {
        this.createdAt = v;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String username, email, password;
        private Role role;
        private boolean blocked = false, deletable = true;

        public Builder username(String v) {
            this.username = v;
            return this;
        }

        public Builder email(String v) {
            this.email = v;
            return this;
        }

        public Builder password(String v) {
            this.password = v;
            return this;
        }

        public Builder role(Role v) {
            this.role = v;
            return this;
        }

        public Builder blocked(boolean v) {
            this.blocked = v;
            return this;
        }

        public Builder deletable(boolean v) {
            this.deletable = v;
            return this;
        }

        public User build() {
            User u = new User();
            u.username = username;
            u.email = email;
            u.password = password;
            u.role = role;
            u.blocked = blocked;
            u.deletable = deletable;
            return u;
        }
    }
}
