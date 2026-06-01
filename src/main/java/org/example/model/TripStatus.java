package org.example.model;

public enum TripStatus {
    NEW("Новый"),
    ARRIVED_LOADING("Прибыл на погрузку"),
    LOADED("Погружен"),
    IN_TRANSIT("В пути"),
    ARRIVED_UNLOADING("Прибыл на выгрузку"),
    UNLOADED("Выгружен"),
    PROCESSED("Обработан");

    private final String displayName;

    TripStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}