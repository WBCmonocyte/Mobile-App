package com.easysport.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "court_resources", indexes = {
        @Index(name = "idx_court_resources_court", columnList = "court_id")
})
public class CourtResource {

    @Id
    @Column(length = 100)
    private String id; // e.g. "court_thanh_phat:g1"

    @Column(name = "court_id", nullable = false, length = 50)
    private String courtId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    public CourtResource() {}

    public CourtResource(String id, String courtId, String name, String description) {
        this.id = id;
        this.courtId = courtId;
        this.name = name;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

