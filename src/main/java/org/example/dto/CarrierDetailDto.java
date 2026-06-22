package org.example.dto;

import org.example.model.Carrier;
import org.example.model.Vehicle;
import java.util.List;

public class CarrierDetailDto {
    private Long id;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private List<VehicleDto> vehicles;

    public static class VehicleDto {
        private Long id;
        private String plateNumber;
        private String brand;
        private String model;
        private String trailerPlate;
        private String driverName;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getPlateNumber() { return plateNumber; }
        public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getTrailerPlate() { return trailerPlate; }
        public void setTrailerPlate(String trailerPlate) { this.trailerPlate = trailerPlate; }
        public String getDriverName() { return driverName; }
        public void setDriverName(String driverName) { this.driverName = driverName; }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<VehicleDto> getVehicles() { return vehicles; }
    public void setVehicles(List<VehicleDto> vehicles) { this.vehicles = vehicles; }

    public static CarrierDetailDto fromEntity(Carrier carrier) {
        CarrierDetailDto dto = new CarrierDetailDto();
        dto.setId(carrier.getId());
        dto.setName(carrier.getName());
        dto.setContactPerson(carrier.getContactPerson());
        dto.setPhone(carrier.getPhone());
        dto.setEmail(carrier.getEmail());
        return dto;
    }
}