package org.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "status_mappings")
public class StatusMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String dispatchStatusName;

    private Long dispatchStatusId;

    private String localStatus;

    private Boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDispatchStatusName() { return dispatchStatusName; }
    public void setDispatchStatusName(String dispatchStatusName) { this.dispatchStatusName = dispatchStatusName; }
    public Long getDispatchStatusId() { return dispatchStatusId; }
    public void setDispatchStatusId(Long dispatchStatusId) { this.dispatchStatusId = dispatchStatusId; }
    public String getLocalStatus() { return localStatus; }
    public void setLocalStatus(String localStatus) { this.localStatus = localStatus; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}