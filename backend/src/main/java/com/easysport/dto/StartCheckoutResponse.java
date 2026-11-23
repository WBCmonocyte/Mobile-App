package com.easysport.dto;

import java.time.LocalDateTime;

public class StartCheckoutResponse {
    private String holdId;
    private String bookingId;
    private String paymentId;
    private String orderInvoiceNumber;
    private String checkoutUrl;
    private Integer amount;
    private Integer totalPrice;
    private Integer depositAmount;
    private Integer remainingAmount;
    private LocalDateTime expiresAt;
    private LocalDateTime paymentDeadline;
    private LocalDateTime verificationDeadline;
    private LocalDateTime serverNow;
    private String holdStatus; // AWAITING_PAYMENT, VERIFYING_PAYMENT, CONFIRMED, EXPIRED, REVIEW_REQUIRED
    private String serverNowIso;
    private String paymentDeadlineIso;
    private String verificationDeadlineIso;

    public StartCheckoutResponse() {}

    public StartCheckoutResponse(String holdId, String bookingId, String paymentId,
                                 String orderInvoiceNumber, String checkoutUrl,
                                 Integer amount, Integer totalPrice, Integer depositAmount,
                                 Integer remainingAmount, LocalDateTime expiresAt,
                                 LocalDateTime serverNow) {
        this.holdId = holdId;
        this.bookingId = bookingId;
        this.paymentId = paymentId;
        this.orderInvoiceNumber = orderInvoiceNumber;
        this.checkoutUrl = checkoutUrl;
        this.amount = amount;
        this.totalPrice = totalPrice;
        this.depositAmount = depositAmount;
        this.remainingAmount = remainingAmount;
        this.expiresAt = expiresAt;
        this.paymentDeadline = expiresAt;
        this.verificationDeadline = expiresAt != null ? expiresAt.plusMinutes(1) : null;
        this.serverNow = serverNow;
        this.holdStatus = "AWAITING_PAYMENT";
        this.serverNowIso = serverNow != null ? com.easysport.service.BookingRules.iso(serverNow) : null;
        this.paymentDeadlineIso = expiresAt != null ? com.easysport.service.BookingRules.iso(expiresAt) : null;
        this.verificationDeadlineIso = this.verificationDeadline != null ? com.easysport.service.BookingRules.iso(verificationDeadline) : null;
    }

    public StartCheckoutResponse(String holdId, String bookingId, String paymentId,
                                 String orderInvoiceNumber, String checkoutUrl,
                                 Integer amount, Integer totalPrice, Integer depositAmount,
                                 Integer remainingAmount, LocalDateTime paymentDeadline,
                                 LocalDateTime verificationDeadline, LocalDateTime serverNow,
                                 String holdStatus) {
        this.holdId = holdId;
        this.bookingId = bookingId;
        this.paymentId = paymentId;
        this.orderInvoiceNumber = orderInvoiceNumber;
        this.checkoutUrl = checkoutUrl;
        this.amount = amount;
        this.totalPrice = totalPrice;
        this.depositAmount = depositAmount;
        this.remainingAmount = remainingAmount;
        this.expiresAt = paymentDeadline;
        this.paymentDeadline = paymentDeadline;
        this.verificationDeadline = verificationDeadline;
        this.serverNow = serverNow;
        this.holdStatus = holdStatus;
        this.serverNowIso = serverNow != null ? com.easysport.service.BookingRules.iso(serverNow) : null;
        this.paymentDeadlineIso = paymentDeadline != null ? com.easysport.service.BookingRules.iso(paymentDeadline) : null;
        this.verificationDeadlineIso = verificationDeadline != null ? com.easysport.service.BookingRules.iso(verificationDeadline) : null;
    }

    public String getHoldId() { return holdId; }
    public void setHoldId(String holdId) { this.holdId = holdId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getOrderInvoiceNumber() { return orderInvoiceNumber; }
    public void setOrderInvoiceNumber(String orderInvoiceNumber) { this.orderInvoiceNumber = orderInvoiceNumber; }

    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public Integer getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }

    public Integer getDepositAmount() { return depositAmount; }
    public void setDepositAmount(Integer depositAmount) { this.depositAmount = depositAmount; }

    public Integer getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(Integer remainingAmount) { this.remainingAmount = remainingAmount; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

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
