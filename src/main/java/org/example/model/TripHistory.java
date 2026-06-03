package org.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_history")
public class TripHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    private String status;

    private String statusCode;

    private String statusDisplay;

    private String changedBy;

    private String userName;

    private LocalDateTime changedAt;

    private String dispatchStatusName;

    private Boolean fromDispatch = false;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }

    public String getStatusDisplay() { return statusDisplay; }
    public void setStatusDisplay(String statusDisplay) { this.statusDisplay = statusDisplay; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    public String getDispatchStatusName() { return dispatchStatusName; }
    public void setDispatchStatusName(String dispatchStatusName) { this.dispatchStatusName = dispatchStatusName; }

    public Boolean getFromDispatch() { return fromDispatch; }
    public void setFromDispatch(Boolean fromDispatch) { this.fromDispatch = fromDispatch; }

    public String getDisplayStatus() {
        if (Boolean.TRUE.equals(fromDispatch) && statusDisplay != null) {
            return statusDisplay;
        }

        if (statusCode != null) {
            switch (statusCode) {
                case "NEW": return "Новый";
                case "ARRIVED_LOADING": return "Прибыл на погрузку";
                case "LOADED": return "Погружен";
                case "IN_TRANSIT": return "В пути";
                case "ARRIVED_UNLOADING": return "Прибыл на выгрузку";
                case "UNLOADED": return "Выгружен";
                case "PROCESSED": return "Обработан";
                case "CANCELLED": return "Отменен";
                case "DELETED": return "Удален";
                default: return statusCode;
            }
        }

        if ("STATUS_UPDATED_FROM_DISPATCH".equals(status)) {
            return statusDisplay != null ? statusDisplay : "Обновление из диспетчеризации";
        }

        switch (status) {
            case "NEW": return "Новый";
            case "ARRIVED_LOADING": return "Прибыл на погрузку";
            case "LOADED": return "Погружен";
            case "IN_TRANSIT": return "В пути";
            case "ARRIVED_UNLOADING": return "Прибыл на выгрузку";
            case "UNLOADED": return "Выгружен";
            case "PROCESSED": return "Обработан";
            case "CANCELLED": return "Отменен";
            case "DELETED": return "Удален";
            default: return status;
        }
    }

    public String getDisplayUserName() {
        if ("dispatch_system".equals(changedBy)) {
            return "Система диспетчеризации";
        }
        if (userName != null && !userName.isEmpty()) {
            return userName;
        }
        return changedBy != null ? changedBy : "Система";
    }
}