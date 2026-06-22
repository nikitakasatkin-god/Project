package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "request_id")
    @JsonIgnore
    private Request request;

    @ManyToOne
    @JoinColumn(name = "carrier_id")
    private Carrier carrier;

    private String vehiclePlate;
    private String trailerPlate;
    private String vehicleBrand;
    private String driverName;
    private LocalDate tripDate;
    private Double volume;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private TripStatusEntity statusEntity;

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    private Boolean syncedToDispatch = false;
    private LocalDateTime syncedAt;
    private Integer sequenceNumber;
    private LocalDateTime createdAt;

    private Long dispatchStatusId;
    private String dispatchStatusName;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    @JsonIgnore
    private List<TripHistory> history = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Request getRequest() { return request; }
    public void setRequest(Request request) { this.request = request; }

    public Carrier getCarrier() { return carrier; }
    public void setCarrier(Carrier carrier) { this.carrier = carrier; }

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }

    public String getTrailerPlate() { return trailerPlate; }
    public void setTrailerPlate(String trailerPlate) { this.trailerPlate = trailerPlate; }

    public String getVehicleBrand() { return vehicleBrand; }
    public void setVehicleBrand(String vehicleBrand) { this.vehicleBrand = vehicleBrand; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public LocalDate getTripDate() { return tripDate; }
    public void setTripDate(LocalDate tripDate) { this.tripDate = tripDate; }

    public Double getVolume() { return volume; }
    public void setVolume(Double volume) { this.volume = volume; }

    public TripStatusEntity getStatusEntity() { return statusEntity; }
    public void setStatusEntity(TripStatusEntity statusEntity) {
        this.statusEntity = statusEntity;
        if (statusEntity != null) {
            try {
                this.status = TripStatus.valueOf(statusEntity.getCode());
            } catch (IllegalArgumentException e) {
                // Пользовательский статус не соответствует enum
            }
        }
    }

    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public Boolean getSyncedToDispatch() { return syncedToDispatch; }
    public void setSyncedToDispatch(Boolean syncedToDispatch) { this.syncedToDispatch = syncedToDispatch; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }

    public Integer getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<TripHistory> getHistory() { return history; }
    public void setHistory(List<TripHistory> history) { this.history = history; }

    public Long getDispatchStatusId() { return dispatchStatusId; }
    public void setDispatchStatusId(Long dispatchStatusId) { this.dispatchStatusId = dispatchStatusId; }

    public String getDispatchStatusName() { return dispatchStatusName; }
    public void setDispatchStatusName(String dispatchStatusName) { this.dispatchStatusName = dispatchStatusName; }

    public String getStatusDisplayName() {
        if (statusEntity != null) {
            return statusEntity.getName();
        }
        if (status != null) {
            return status.getDisplayName();
        }
        return "Неизвестно";
    }
}