package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "division_id")
    @JsonIgnore
    private Division division;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    private Double volume;

    private String pickupPoint;

    private String deliveryPoint;

    private LocalDate pickupStartDate;

    private LocalDate pickupEndDate;

    private LocalTime pickupStartTime;

    private LocalTime pickupEndTime;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.NEW;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Trip> trips = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public Division getDivision() { return division; }
    public void setDivision(Division division) { this.division = division; }
    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }
    public Double getVolume() { return volume; }
    public void setVolume(Double volume) { this.volume = volume; }
    public String getPickupPoint() { return pickupPoint; }
    public void setPickupPoint(String pickupPoint) { this.pickupPoint = pickupPoint; }
    public String getDeliveryPoint() { return deliveryPoint; }
    public void setDeliveryPoint(String deliveryPoint) { this.deliveryPoint = deliveryPoint; }
    public LocalDate getPickupStartDate() { return pickupStartDate; }
    public void setPickupStartDate(LocalDate pickupStartDate) { this.pickupStartDate = pickupStartDate; }
    public LocalDate getPickupEndDate() { return pickupEndDate; }
    public void setPickupEndDate(LocalDate pickupEndDate) { this.pickupEndDate = pickupEndDate; }
    public LocalTime getPickupStartTime() { return pickupStartTime; }
    public void setPickupStartTime(LocalTime pickupStartTime) { this.pickupStartTime = pickupStartTime; }
    public LocalTime getPickupEndTime() { return pickupEndTime; }
    public void setPickupEndTime(LocalTime pickupEndTime) { this.pickupEndTime = pickupEndTime; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public List<Trip> getTrips() { return trips; }
    public void setTrips(List<Trip> trips) { this.trips = trips; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}