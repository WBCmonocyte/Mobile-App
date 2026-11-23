package com.easysport.dto;

import java.time.LocalDateTime;

public class InitiatePaymentResponse {
    private String paymentId;
    private String bookingId;
    private String orderInvoiceNumber;
    private Integer amount;
    private String currency;
    private String checkoutUrl;
    private LocalDateTime expiresAt;

    public InitiatePaymentResponse() {}

    public InitiatePaymentResponse(String paymentId, String bookingId, String orderInvoiceNumber,
                                  Integer amount, String currency, String checkoutUrl, LocalDateTime expiresAt) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.orderInvoiceNumber = orderInvoiceNumber;
        this.amount = amount;
        this.currency = currency;
        this.checkoutUrl = checkoutUrl;
        this.expiresAt = expiresAt;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getOrderInvoiceNumber() { return orderInvoiceNumber; }
    public void setOrderInvoiceNumber(String orderInvoiceNumber) { this.orderInvoiceNumber = orderInvoiceNumber; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}

