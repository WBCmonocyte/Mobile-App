package com.easysport;

import com.easysport.config.SepayConfig;
import com.easysport.entity.*;
import com.easysport.repository.*;
import com.easysport.service.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SepayServiceTest {
    PaymentRepository payments=mock(PaymentRepository.class);
    BookingRepository bookings=mock(BookingRepository.class);
    CourtRepository courts=mock(CourtRepository.class);
    CourtHoldRepository holds=mock(CourtHoldRepository.class);
    CourtHoldSlotRepository slots=mock(CourtHoldSlotRepository.class);
    Clock clock=Clock.fixed(Instant.parse("2026-11-15T00:00:00Z"),BookingRules.ZONE);
    Payment payment; Booking booking; CourtHold hold; SepayService service;
    @BeforeEach void setup() {
        SepayConfig config=new SepayConfig();config.setMerchantId("test");config.setSecretKey("secret");config.setIpnSecret("ipn");
        service=new SepayService(config,payments,bookings,courts,holds,slots,clock);
        var now=LocalDateTime.now(clock);
        payment=new Payment("p1","b1","invoice",300000,"VND","QR_TRANSFER","token",now.plusMinutes(5));
        booking=new Booking();booking.setId("b1");booking.setCourtId("c1");booking.setStatus("PENDING");
        hold=new CourtHold("h1","b1","p1","u1","c1","f1","Field","16/11/2026","19:00 - 20:30",
                "19:00 - 20:30","r1",now,now.plusMinutes(5),now.plusMinutes(6),"key");
        when(payments.findByOrderInvoiceNumber("invoice")).thenReturn(Optional.of(payment));
        when(payments.lockById("p1")).thenReturn(Optional.of(payment));
        when(bookings.findById("b1")).thenReturn(Optional.of(booking));
        when(courts.lockById("c1")).thenReturn(Optional.of(new Court()));
        when(holds.findByBookingId("b1")).thenReturn(Optional.of(hold));
        when(holds.findByIdWithLock("h1")).thenReturn(Optional.of(hold));
    }
    Map<String,Object> payload(String amount) {
        return Map.of("notification_type","ORDER_PAID","order",Map.of("order_status","CAPTURED",
                "order_invoice_number","invoice","order_currency","VND","order_amount",amount),
                "transaction",Map.of("id","tx1","transaction_type","PAYMENT","transaction_status","APPROVED",
                "transaction_amount",amount,"transaction_currency","VND"));
    }
    @Test void decimalAmountIsNotMultipliedByOneHundred() {
        assertTrue(service.processIpn("ipn",payload("300000.00")));
        assertEquals(300000,payment.getConfirmedAmount());assertEquals("PAID",booking.getStatus());
    }
    @Test void wrongSecretIsRejectedWithoutWrites() {
        assertFalse(service.processIpn("wrong",payload("300000")));verifyNoInteractions(payments);
    }
    @Test void sandboxSetupProbeRequiresAuthenticationAndNeverTouchesBookings() {
        Map<String, Object> probe = Map.of(
                "notification_type", "PAYMENT_SUCCESS",
                "order", Map.of("order_id", "TEST_ORDER_1", "order_invoice_number", "invoice",
                        "custom_data", Map.of("test_mode", true, "webhook_test", true)),
                "transaction", Map.of("transaction_id", "TEST_TXN_1", "transaction_type", "CAPTURE"));
        assertFalse(service.processIpn("wrong", probe));
        assertTrue(service.processIpn("ipn", probe));
        assertFalse(service.processIpn("ipn", Map.of("notification_type", "PAYMENT_SUCCESS")));
        verifyNoInteractions(payments, bookings, courts, holds, slots);
    }
    @Test void wrongAmountDoesNotPoisonExistingPayment() {
        assertFalse(service.processIpn("ipn",payload("300001")));assertEquals("PENDING",payment.getStatus());
    }
    @Test void fractionalAndOverflowAmountsAreRejected() {
        assertFalse(service.processIpn("ipn",payload("300000.01")));
        assertFalse(service.processIpn("ipn",payload("99999999999999")));
    }
    @Test void malformedPayloadIsRejected() {
        assertFalse(service.processIpn("ipn",Map.of("notification_type","ORDER_PAID","order","bad")));
    }
    @Test void duplicateIsIdempotent() {
        payment.setSepayTransactionId("tx1");payment.setStatus("PAID");
        assertTrue(service.processIpn("ipn",payload("300000")));verify(payments,never()).save(any());
    }
    @Test void transactionCannotBeReusedForAnotherOrder() {
        when(payments.existsBySepayTransactionId("tx1")).thenReturn(true);
        assertFalse(service.processIpn("ipn",payload("300000")));verify(payments,never()).save(any());
    }
    @Test void cancelledHoldCannotBeRevivedEvenBeforeDeadline() {
        hold.setStatus("RELEASED");booking.setStatus("CANCELLED");
        assertTrue(service.processIpn("ipn",payload("300000")));
        assertEquals("REVIEW_REQUIRED",payment.getStatus());assertNotEquals("CONFIRMED",hold.getStatus());
    }
    @Test void exactSixMinuteBoundaryRequiresReview() {
        hold.setVerificationDeadline(LocalDateTime.now(clock));
        assertTrue(service.processIpn("ipn",payload("300000")));assertEquals("REVIEW_REQUIRED",payment.getStatus());
    }
    @Test void duringVerificationCanConfirm() {
        hold.setPaymentDeadline(LocalDateTime.now(clock).minusSeconds(30));
        hold.setVerificationDeadline(LocalDateTime.now(clock).plusSeconds(30));
        assertTrue(service.processIpn("ipn",payload("300000")));assertEquals("CONFIRMED",hold.getStatus());
    }
    @Test void depositMarksDepositPaid() {
        payment.setPaymentMethod("CASH_ON_SITE");
        assertTrue(service.processIpn("ipn",payload("300000")));assertEquals("DEPOSIT_PAID",booking.getStatus());
    }
    @Test void checkoutHtmlDoesNotDoubleEscapeDescription() {
        booking.setCourtName("A & B");booking.setFieldDetail("Field");
        String html=service.renderCheckoutHtml(payment,booking);
        assertTrue(html.contains("A &amp; B"));assertFalse(html.contains("A &amp;amp; B"));
    }

    @Test void checkoutPostsFieldsInSepayDocumentedOrder() {
        String html = service.renderCheckoutHtml(payment, booking);
        var inputs = java.util.regex.Pattern.compile("<input type=\"hidden\" name=\"([^\"]+)\"")
                .matcher(html).results().map(match -> match.group(1)).toList();

        assertEquals(List.of("order_amount", "merchant", "currency", "operation", "order_description",
                "order_invoice_number", "success_url", "error_url", "cancel_url", "signature"), inputs);
    }
}
