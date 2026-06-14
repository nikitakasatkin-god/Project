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

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Double volume;

    // ========== ИСПРАВЛЕНО: теперь это связи с таблицами ==========
    @ManyToOne
    @JoinColumn(name = "pickup_plant_id")
    private Plant pickupPlant;           // вместо pickupPoint (String)

    @ManyToOne
    @JoinColumn(name = "delivery_warehouse_id")
    private Warehouse deliveryWarehouse;  // вместо deliveryPoint (String)
    // =============================================================

    private LocalDate pickupStartDate;
    private LocalDate pickupEndDate;
    private LocalTime pickupStartTime;
    private LocalTime pickupEndTime;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.NEW;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Trip> trips = new ArrayList<>();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    @JsonIgnore
    private List<RequestHistory> history = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ========== GETTERS AND SETTERS ==========
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public Division getDivision() { return division; }
    public void setDivision(Division division) { this.division = division; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Double getVolume() { return volume; }
    public void setVolume(Double volume) { this.volume = volume; }

    public Plant getPickupPlant() { return pickupPlant; }
    public void setPickupPlant(Plant pickupPlant) { this.pickupPlant = pickupPlant; }

    public Warehouse getDeliveryWarehouse() { return deliveryWarehouse; }
    public void setDeliveryWarehouse(Warehouse deliveryWarehouse) { this.deliveryWarehouse = deliveryWarehouse; }

    // Вспомогательные методы для совместимости с существующим кодом
    @Transient
    public String getPickupPoint() {
        return pickupPlant != null ? pickupPlant.getName() : null;
    }

    @Transient
    public String getDeliveryPoint() {
        return deliveryWarehouse != null ? deliveryWarehouse.getName() : null;
    }

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

    public List<RequestHistory> getHistory() { return history; }
    public void setHistory(List<RequestHistory> history) { this.history = history; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}