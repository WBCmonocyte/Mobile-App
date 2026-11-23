package com.easysport.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "court_fields")
public class CourtField {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false)
    @JsonBackReference
    private Court court;

    @Column(name = "field_label", nullable = false, length = 50)
    private String label;

    @Column(name = "field_detail", nullable = false, length = 100)
    private String detail;

    @Column(name = "field_type", length = 30)
    private String fieldType = "5_VS_5";

    @Column(name = "resource_keys", length = 255)
    private String resourceKeys;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public CourtField() {}

    public CourtField(String id, Court court, String label, String detail, String fieldType) {
        this(id, court, label, detail, fieldType, null);
    }

    public CourtField(String id, Court court, String label, String detail, String fieldType, String resourceKeys) {
        this.id = id;
        this.court = court;
        this.label = label;
        this.detail = detail;
        this.fieldType = fieldType;
        this.resourceKeys = resourceKeys;
        this.isActive = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Court getCourt() { return court; }
    public void setCourt(Court court) { this.court = court; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }

    public String getResourceKeys() { return resourceKeys; }
    public void setResourceKeys(String resourceKeys) { this.resourceKeys = resourceKeys; }

    public Boolean getActive() { return isActive; }
    public void setActive(Boolean active) { isActive = active; }
}
