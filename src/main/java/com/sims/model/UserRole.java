package com.sims.model;

/**
 * Enumeration of RBAC roles supported by SIMS.
 *
 * <p>Values match the CHECK constraint in {@code USERS.ROLE} column
 * ({@code 'ADMIN','FACULTY','STUDENT','PARENT'}).
 */
public enum UserRole {
    ADMIN,
    FACULTY,
    STUDENT,
    PARENT;

    /**
     * Case-insensitive parse helper used after reading the role string
     * from the database {@code ResultSet}.
     */
    public static UserRole fromString(String value) {
        return UserRole.valueOf(value.toUpperCase());
    }
}
