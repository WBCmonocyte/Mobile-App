package com.easysport.dto;

public class TournamentResponse {
    private String id;
    private String title;
    private String courtId;
    private String courtName;
    private String address;
    private String startsAt;
    private String endsAt;
    private Integer maxTeams;
    private Boolean isDemo;

    public TournamentResponse() {}

    public TournamentResponse(String id, String title, String courtId, String courtName, String address,
                              String startsAt, String endsAt, Integer maxTeams, Boolean isDemo) {
        this.id = id;
        this.title = title;
        this.courtId = courtId;
        this.courtName = courtName;
        this.address = address;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCourtId() {
        return courtId;
    }

    public void setCourtId(String courtId) {
        this.courtId = courtId;
    }

    public String getCourtName() {
        return courtName;
    }

    public void setCourtName(String courtName) {
        this.courtName = courtName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(String startsAt) {
        this.startsAt = startsAt;
    }

    public String getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(String endsAt) {
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
