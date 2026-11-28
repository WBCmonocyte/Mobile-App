package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_invoice", columnList = "order_invoice_number", unique = true),
        @Index(name = "idx_payments_checkout_token", columnList = "checkout_token", unique = true),
        @Index(name = "idx_payments_booking", columnList = "booking_id")
})
public class Payment {

    @Version
    private Long version;

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "booking_id", nullable = false, length = 64)
    private String bookingId;

    @Column(name = "order_invoice_number", nullable = false, unique = true, length = 64)
    private String orderInvoiceNumber;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "confirmed_amount")
    private Integer confirmedAmount;

    @Column(length = 10, nullable = false)
    private String currency = "VND";

    @Column(name = "payment_method", length = 30, nullable = false)
    private String paymentMethod = "QR_TRANSFER";

    @Column(length = 30, nullable = false)
    private String status = "PENDING";

    @Column(name = "sepay_transaction_id", length = 100, unique = true)
    private String sepayTransactionId;

    @Column(name = "checkout_token", length = 64, unique = true)
    private String checkoutToken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "raw_ipn_data", length = 4000)
    private String rawIpnData;

    @Column(name = "recurring_package_id", length = 64)
    private String recurringPackageId;

    public Payment() {}

    public Payment(String id, String bookingId, String orderInvoiceNumber, Integer amount,
                   String currency, String paymentMethod, String checkoutToken,
                   LocalDateTime expiresAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.orderInvoiceNumber = orderInvoiceNumber;
        this.amount = amount;
        this.currency = currency != null ? currency : "VND";
        this.paymentMethod = paymentMethod;
        this.status = "PENDING";
        this.checkoutToken = checkoutToken;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt != null ? expiresAt : this.createdAt.plusMinutes(15);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getOrderInvoiceNumber() { return orderInvoiceNumber; }
    public void setOrderInvoiceNumber(String orderInvoiceNumber) { this.orderInvoiceNumber = orderInvoiceNumber; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public Integer getConfirmedAmount() { return confirmedAmount; }
    public void setConfirmedAmount(Integer confirmedAmount) { this.confirmedAmount = confirmedAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSepayTransactionId() { return sepayTransactionId; }
    public void setSepayTransactionId(String sepayTransactionId) { this.sepayTransactionId = sepayTransactionId; }

    public String getCheckoutToken() { return checkoutToken; }
    public void setCheckoutToken(String checkoutToken) { this.checkoutToken = checkoutToken; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public String getRawIpnData() { return rawIpnData; }
    public void setRawIpnData(String rawIpnData) { this.rawIpnData = rawIpnData; }

    public String getRecurringPackageId() { return recurringPackageId; }
    public void setRecurringPackageId(String recurringPackageId) { this.recurringPackageId = recurringPackageId; }
}

