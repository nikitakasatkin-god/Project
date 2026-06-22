package org.example.model;

public enum Role {
    ADMIN("Администратор"),
    LOGIST("Логист"),
    DISPATCHER("Диспетчер");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}