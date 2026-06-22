package org.example.model;

public enum RequestStatus {
    NEW("Новая"),
    IN_PROGRESS("В работе"),
    PROCESSED("Обработана"),
    COMPLETED("Завершена"),
    REJECTED("Отклонена");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}