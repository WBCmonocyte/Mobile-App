package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "max_teams", nullable = false)
    private Integer maxTeams;

    @Column(name = "is_demo", nullable = false)
    private Boolean isDemo = false;

    public Tournament() {}

    public Tournament(String id, Court court, String title, LocalDateTime startsAt, LocalDateTime endsAt, Integer maxTeams, Boolean isDemo) {
        this.id = id;
        this.court = court;
        this.title = title;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.maxTeams = maxTeams;
        this.isDemo = isDemo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Court getCourt() {
        return court;
    }

    public void setCourt(Court court) {
        this.court = court;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(LocalDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public Integer getMaxTeams() {
        return maxTeams;
    }

    public void setMaxTeams(Integer maxTeams) {
        this.maxTeams = maxTeams;
    }

    public Boolean getIsDemo() {
        return isDemo;
    }

    public void setIsDemo(Boolean isDemo) {
        this.isDemo = isDemo;
    }
}
