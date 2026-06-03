package org.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_statuses")
public class TripStatusEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    private String color;

    private Integer sortOrder;

    private Boolean active = true;

    private Boolean systemDefault = false;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getSystemDefault() { return systemDefault; }
    public void setSystemDefault(Boolean systemDefault) { this.systemDefault = systemDefault; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}