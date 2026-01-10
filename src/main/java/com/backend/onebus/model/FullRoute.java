package com.backend.onebus.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "full_routes")
@Getter
@Setter
public class FullRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "direction", length = 50)
    private String direction;

    @Column(name = "description", length = 500)
    private String description;

    // Store coordinates as JSON text: [{"lat":...,"lon":...}, ...]
    @Column(name = "coordinates_json", nullable = false, columnDefinition = "TEXT")
    private String coordinatesJson;

    // Store pre-calculated cumulative distances in meters: [0, 50.2, 125.8, ...]
    @Column(name = "cumulative_distances_json", columnDefinition = "TEXT")
    private String cumulativeDistancesJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
