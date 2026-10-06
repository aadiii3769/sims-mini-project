package com.sims.model;

import java.time.LocalDateTime;

/**
 * POJO mapping to the {@code USERS} database table.
 *
 * <p>Plain Java Object — no framework annotations, no ORM magic.
 * All fields map 1-to-1 with {@code USERS} columns.
 */
public class User {

    private long          userId;
    private String        username;
    private String        passwordHash;   // BCrypt hash — never plain-text in prod
    private String        fullName;
    private String        email;
    private String        phone;
    private UserRole      role;
    private boolean       active;
    private LocalDateTime createdAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    public User() {}

    public User(long userId, String username, String fullName,
                String email, String phone, UserRole role, boolean active) {
        this.userId   = userId;
        this.username = username;
        this.fullName = fullName;
        this.email    = email;
        this.phone    = phone;
        this.role     = role;
        this.active   = active;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getUserId()                     { return userId; }
    public void setUserId(long userId)          { this.userId = userId; }

    public String getUsername()                 { return username; }
    public void setUsername(String username)    { this.username = username; }

    public String getPasswordHash()             { return passwordHash; }
    public void setPasswordHash(String hash)    { this.passwordHash = hash; }

    public String getFullName()                 { return fullName; }
    public void setFullName(String fullName)    { this.fullName = fullName; }

    public String getEmail()                    { return email; }
    public void setEmail(String email)          { this.email = email; }

    public String getPhone()                    { return phone; }
    public void setPhone(String phone)          { this.phone = phone; }

    public UserRole getRole()                   { return role; }
    public void setRole(UserRole role)          { this.role = role; }

    public boolean isActive()                   { return active; }
    public void setActive(boolean active)       { this.active = active; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime dt)  { this.createdAt = dt; }

    @Override
    public String toString() {
        return "User{id=" + userId + ", username='" + username +
               "', role=" + role + ", active=" + active + '}';
    }
}
