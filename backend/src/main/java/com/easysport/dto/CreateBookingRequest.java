package com.easysport.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateBookingRequest {

    private String userId;

    @NotBlank(message = "Thiếu thông tin sân")
    private String courtId;

    private String courtName;

    @NotBlank(message = "Thiếu chi tiết sân đấu")
    private String fieldDetail;

    @NotBlank(message = "Thiếu khung giờ thuê")
    private String timeRange;

    @NotBlank(message = "Thiếu ngày thuê")
    private String dateLabel;

    private List<String> slots;

    // Giá tiền sẽ do server tự tính toán từ database, không tin số tiền client gửi
    private Integer totalPrice;
    private Integer depositAmount;

    private String paymentMethod = "QR_TRANSFER";

    @NotBlank(message = "Vui lòng nhập họ tên người đặt")
    private String customerName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    private String customerPhone;

    private String address = "";

    public CreateBookingRequest() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public String getFieldDetail() { return fieldDetail; }
    public void setFieldDetail(String fieldDetail) { this.fieldDetail = fieldDetail; }

    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

    public String getDateLabel() { return dateLabel; }
    public void setDateLabel(String dateLabel) { this.dateLabel = dateLabel; }

    public List<String> getSlots() { return slots; }
    public void setSlots(List<String> slots) { this.slots = slots; }

    public Integer getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }

    public Integer getDepositAmount() { return depositAmount; }
    public void setDepositAmount(Integer depositAmount) { this.depositAmount = depositAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
