package org.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispatch_status_mappings")
public class DispatchStatusMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long dispatchStatusId;

    private String dispatchStatusName;

    @ManyToOne
    @JoinColumn(name = "local_status_id", nullable = false)
    private TripStatusEntity localStatus;

    private Boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDispatchStatusId() { return dispatchStatusId; }
    public void setDispatchStatusId(Long dispatchStatusId) { this.dispatchStatusId = dispatchStatusId; }
    public String getDispatchStatusName() { return dispatchStatusName; }
    public void setDispatchStatusName(String dispatchStatusName) { this.dispatchStatusName = dispatchStatusName; }
    public TripStatusEntity getLocalStatus() { return localStatus; }
    public void setLocalStatus(TripStatusEntity localStatus) { this.localStatus = localStatus; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}