package com.easysport.dto;

import java.time.LocalDateTime;

public class BookingPaymentStatusResponse {
    private String bookingId;
    private String bookingStatus;
    private Integer totalPrice;
    private Integer depositAmount;
    private Integer remainingAmount;
    private String paymentMethod;
    private boolean isPaid;
    private boolean isDepositPaid;
    private boolean isExpired;

    private String paymentId;
    private String orderInvoiceNumber;
    private String paymentStatus;
    private Integer paymentAmount;
    private String checkoutUrl;
    private LocalDateTime expiresAt;
    private LocalDateTime paymentDeadline;
    private LocalDateTime verificationDeadline;
    private LocalDateTime serverNow;
    private String holdStatus; // AWAITING_PAYMENT, VERIFYING_PAYMENT, CONFIRMED, EXPIRED, REVIEW_REQUIRED
    private String serverNowIso;
    private String paymentDeadlineIso;
    private String verificationDeadlineIso;

    public BookingPaymentStatusResponse() {}

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public Integer getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }

    public Integer getDepositAmount() { return depositAmount; }
    public void setDepositAmount(Integer depositAmount) { this.depositAmount = depositAmount; }

    public Integer getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(Integer remainingAmount) { this.remainingAmount = remainingAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { this.isPaid = paid; }

    public boolean isDepositPaid() { return isDepositPaid; }
    public void setDepositPaid(boolean depositPaid) { this.isDepositPaid = depositPaid; }

    public boolean isExpired() { return isExpired; }
    public void setExpired(boolean expired) { this.isExpired = expired; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getOrderInvoiceNumber() { return orderInvoiceNumber; }
    public void setOrderInvoiceNumber(String orderInvoiceNumber) { this.orderInvoiceNumber = orderInvoiceNumber; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Integer getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(Integer paymentAmount) { this.paymentAmount = paymentAmount; }

    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        if (this.paymentDeadline == null) {
            this.paymentDeadline = expiresAt;
        }
    }

    public LocalDateTime getPaymentDeadline() { return paymentDeadline; }
    public void setPaymentDeadline(LocalDateTime paymentDeadline) { this.paymentDeadline = paymentDeadline; }

    public LocalDateTime getVerificationDeadline() { return verificationDeadline; }
    public void setVerificationDeadline(LocalDateTime verificationDeadline) { this.verificationDeadline = verificationDeadline; }

    public LocalDateTime getServerNow() { return serverNow; }
    public void setServerNow(LocalDateTime serverNow) { this.serverNow = serverNow; }

    public String getHoldStatus() { return holdStatus; }
    public void setHoldStatus(String holdStatus) { this.holdStatus = holdStatus; }

    public String getServerNowIso() { return serverNowIso; }
    public void setServerNowIso(String serverNowIso) { this.serverNowIso = serverNowIso; }

    public String getPaymentDeadlineIso() { return paymentDeadlineIso; }
    public void setPaymentDeadlineIso(String paymentDeadlineIso) { this.paymentDeadlineIso = paymentDeadlineIso; }

    public String getVerificationDeadlineIso() { return verificationDeadlineIso; }
    public void setVerificationDeadlineIso(String verificationDeadlineIso) { this.verificationDeadlineIso = verificationDeadlineIso; }
}
