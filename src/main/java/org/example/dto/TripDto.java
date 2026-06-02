package org.example.dto;

import org.example.model.TripStatus;
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
    private TripStatus status;
    private Boolean syncedToDispatch;
    private Integer sequenceNumber;

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

    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }

    public Boolean getSyncedToDispatch() { return syncedToDispatch; }
    public void setSyncedToDispatch(Boolean syncedToDispatch) { this.syncedToDispatch = syncedToDispatch; }

    public Integer getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }
}