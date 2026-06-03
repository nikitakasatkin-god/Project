package org.example.dto;

import java.time.LocalDate;

public class TripDto {
    private Long id;
    private Long requestId;
    private String carrierName;
    private String vehiclePlate;
    private String trailerPlate;
    private String vehicleBrand;
    private String driverName;
    private LocalDate tripDate;
    private Double volume;
    private String status;
    private String statusDisplayName;
    private String statusColor;
    private Boolean syncedToDispatch;
    private Integer sequenceNumber;
    private String dispatchStatusName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public String getCarrierName() { return carrierName; }
    public void setCarrierName(String carrierName) { this.carrierName = carrierName; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusDisplayName() { return statusDisplayName; }
    public void setStatusDisplayName(String statusDisplayName) { this.statusDisplayName = statusDisplayName; }

    public String getStatusColor() { return statusColor; }
    public void setStatusColor(String statusColor) { this.statusColor = statusColor; }

    public Boolean getSyncedToDispatch() { return syncedToDispatch; }
    public void setSyncedToDispatch(Boolean syncedToDispatch) { this.syncedToDispatch = syncedToDispatch; }

    public Integer getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public String getDispatchStatusName() { return dispatchStatusName; }
    public void setDispatchStatusName(String dispatchStatusName) { this.dispatchStatusName = dispatchStatusName; }
}