package com.easysport.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courts")
public class Court {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    private Double rating = 5.0;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "distance_km")
    private Double distanceKm = 1.0;

    private Double latitude;

    private Double longitude;

    @Column(name = "sport_type", length = 50)
    private String sportType = "Bóng đá";

    @Column(name = "indoor")
    private Boolean indoor = false;

    @Column(name = "price_per_hour")
    private Integer pricePerHour = 300000;

    @Column(name = "morning_price")
    private Integer morningPrice = 150000;

    @Column(name = "afternoon_price")
    private Integer afternoonPrice = 250000;

    @Column(name = "evening_price")
    private Integer eveningPrice = 300000;

    @Column(name = "image_res_name")
    private String imageResName = "court_thanh_phat";

    @OneToMany(mappedBy = "court", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<CourtField> fields = new ArrayList<>();

    public Court() {}

    public Court(String id, String name, String address, Double rating, Integer reviewCount, Double distanceKm, Integer pricePerHour, Integer morningPrice, Integer afternoonPrice, Integer eveningPrice, String imageResName) {
        this(id, name, address, rating, reviewCount, distanceKm, pricePerHour, morningPrice, afternoonPrice, eveningPrice, imageResName, null, null, "Bóng đá", false);
    }

    public Court(String id, String name, String address, Double rating, Integer reviewCount, Double distanceKm, Integer pricePerHour, Integer morningPrice, Integer afternoonPrice, Integer eveningPrice, String imageResName, Double latitude, Double longitude, String sportType, Boolean indoor) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.distanceKm = distanceKm;
        this.pricePerHour = pricePerHour;
        this.morningPrice = morningPrice;
        this.afternoonPrice = afternoonPrice;
        this.eveningPrice = eveningPrice;
        this.imageResName = imageResName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sportType = (sportType != null) ? sportType : "Bóng đá";
        this.indoor = (indoor != null) ? indoor : false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getSportType() { return sportType; }
    public void setSportType(String sportType) { this.sportType = sportType; }

    public Boolean getIndoor() { return indoor; }
    public void setIndoor(Boolean indoor) { this.indoor = indoor; }

    public Integer getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(Integer pricePerHour) { this.pricePerHour = pricePerHour; }

    public Integer getMorningPrice() { return morningPrice; }
    public void setMorningPrice(Integer morningPrice) { this.morningPrice = morningPrice; }

    public Integer getAfternoonPrice() { return afternoonPrice; }
    public void setAfternoonPrice(Integer afternoonPrice) { this.afternoonPrice = afternoonPrice; }

    public Integer getEveningPrice() { return eveningPrice; }
    public void setEveningPrice(Integer eveningPrice) { this.eveningPrice = eveningPrice; }

    public String getImageResName() { return imageResName; }
    public void setImageResName(String imageResName) { this.imageResName = imageResName; }

    public List<CourtField> getFields() { return fields; }
    public void setFields(List<CourtField> fields) { this.fields = fields; }
}

