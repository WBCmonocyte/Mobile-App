package com.easysport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class StartCheckoutRequest {

    @NotBlank(message = "courtId không được để trống")
    private String courtId;

    @NotBlank(message = "fieldDetail không được để trống")
    private String fieldDetail;

    @NotEmpty(message = "Danh sách khung giờ không được để trống")
    private List<String> slots;

    @NotBlank(message = "dateLabel không được để trống")
    private String dateLabel;

    private String paymentMethod = "QR_TRANSFER";
    private String customerName;
    private String customerPhone;
    private String address;
    private String userId;

    private String idempotencyKey;

    public StartCheckoutRequest() {}

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getFieldDetail() { return fieldDetail; }
    public void setFieldDetail(String fieldDetail) { this.fieldDetail = fieldDetail; }

    public List<String> getSlots() { return slots; }
    public void setSlots(List<String> slots) { this.slots = slots; }

    public String getDateLabel() { return dateLabel; }
    public void setDateLabel(String dateLabel) { this.dateLabel = dateLabel; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}

