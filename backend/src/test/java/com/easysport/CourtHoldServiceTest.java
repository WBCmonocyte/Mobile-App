package com.easysport;

import com.easysport.config.SepayConfig;
import com.easysport.controller.BookingController;
import com.easysport.dto.*;
import com.easysport.entity.*;
import com.easysport.repository.*;
import com.easysport.service.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CourtHoldServiceTest {
    CourtRepository courts = mock(CourtRepository.class);
    CourtFieldRepository fields = mock(CourtFieldRepository.class);
    CourtResourceRepository resources = mock(CourtResourceRepository.class);
    CourtHoldRepository holds = mock(CourtHoldRepository.class);
    CourtHoldSlotRepository slots = mock(CourtHoldSlotRepository.class);
    BookingRepository bookings = mock(BookingRepository.class);
    PaymentRepository payments = mock(PaymentRepository.class);
    AuthService auth;
    Clock clock = Clock.fixed(Instant.parse("2026-11-15T00:00:00Z"), BookingRules.ZONE);
    SepayConfig config = new SepayConfig();
    Court court;
    CourtHoldService service;
    BookingController controller;
    @BeforeEach void setup() {
        auth = new AuthService(null, null, clock) {
            @Override
            public User authenticate(String token) {
                if ("Bearer valid".equals(token)) {
                    User u = new User();
                    u.setId("u1");
                    return u;
                }
                return null;
            }
        };
        court = new Court("c1", "Court", "Address", 4.5, 1, 1.0, 300000, 150000, 250000, 300000, "img");
        court.setFields(List.of(new CourtField("f1",court,"Field 1","Field one","5_VS_5","r1")));
        when(courts.findById("c1")).thenReturn(Optional.of(court));
        when(courts.lockById("c1")).thenReturn(Optional.of(court));
        when(resources.existsById("r1")).thenReturn(true);
        SepayService sepay = new SepayService(config,payments,bookings,courts,holds,slots,clock);
        service = new CourtHoldService(courts,fields,resources,holds,slots,bookings,payments,sepay,config,clock);
        controller = new BookingController(bookings,courts,payments,holds,service,sepay,config,clock,auth);
    }
    StartCheckoutRequest request() {
        StartCheckoutRequest r = new StartCheckoutRequest();
        r.setCourtId("c1"); r.setFieldDetail("f1"); r.setDateLabel("16/11/2026");
        r.setUserId("u1"); r.setIdempotencyKey("key"); r.setSlots(List.of("19:00 - 20:30"));
        r.setCustomerName("User"); r.setCustomerPhone("0901234567"); return r;
    }
    CourtHold existing() {
        var n = LocalDateTime.now(clock);
        CourtHold h = new CourtHold("h1","b1","p1","u1","c1","f1","Field one","16/11/2026",
                "19:00 - 20:30","19:00 - 20:30","r1",n,n.plusMinutes(5),n.plusMinutes(6),"key");
        when(holds.findById("h1")).thenReturn(Optional.of(h));
        when(holds.findByIdWithLock("h1")).thenReturn(Optional.of(h)); return h;
    }
    @Test void deadlinesAndAuthoritativePrice() {
        var result = service.startCheckout(request(),"u1");
        assertEquals(300000,result.getAmount());
        assertEquals(LocalDateTime.now(clock).plusMinutes(5),result.getPaymentDeadline());
        assertEquals(LocalDateTime.now(clock).plusMinutes(6),result.getVerificationDeadline());
        verify(courts).lockById("c1");
    }
    @Test void spoofedUserIdCannotAuthenticate() {
        assertThrows(ResponseStatusException.class, () -> controller.startCheckout(request(),null,"u1"));
        verify(holds,never()).save(any());
    }
    @Test void anonymousCannotListAllBookings() {
        assertThrows(ResponseStatusException.class, () -> controller.getBookings("u1",null,"u1"));
        verify(bookings,never()).findAllByOrderByCreatedAtDesc();
    }
    @Test void bookingOwnerIsRequired() {
        Booking b = new Booking(); b.setUserId("u2"); when(bookings.findById("b1")).thenReturn(Optional.of(b));
        assertEquals(403,controller.getBookingById("b1","Bearer valid",null).getStatusCode().value());
    }
    @Test void invalidFieldDoesNotFallBackToFirst() {
        var r=request(); r.setFieldDetail("nonexistent");
        assertThrows(IllegalArgumentException.class, () -> service.startCheckout(r,"u1"));
    }
    @Test void inactiveFieldCannotBeBooked() {
        court.getFields().get(0).setActive(false);
        assertThrows(IllegalArgumentException.class, () -> service.startCheckout(request(),"u1"));
    }
    @Test void multiSlotConflictDoesNotWriteAnyHold() {
        var r=request();r.setSlots(List.of("07:00 - 08:30","08:30 - 10:00"));
        when(slots.findOverlappingActiveSlots(anyList(),anyString(),eq(510),eq(600),any()))
                .thenReturn(List.of(new CourtHoldSlot("s","other","c1","r1","16/11/2026",510,600,"08:30 - 10:00","AWAITING_PAYMENT",LocalDateTime.now(clock).plusMinutes(6))));
        assertThrows(SlotConflictException.class, () -> service.startCheckout(r,"u1"));
        verify(holds,never()).save(any());
    }
    @Test void idempotentRetryKeepsOriginalDeadlines() {
        CourtHold h=existing(); Booking b=new Booking();b.setPaymentMethod("QR_TRANSFER");
        Payment p=new Payment();p.setCheckoutToken("token");
        when(holds.findByIdempotencyKey(anyString())).thenReturn(Optional.of(h));
        when(bookings.findById("b1")).thenReturn(Optional.of(b));when(payments.findById("p1")).thenReturn(Optional.of(p));
        assertEquals("h1",service.startCheckout(request(),"u1").getHoldId());
        verify(holds,never()).save(any());
    }
    @Test void idempotencyKeyCannotChangeSelection() {
        CourtHold h=existing();Booking b=new Booking();b.setPaymentMethod("CASH_ON_SITE");
        when(holds.findByIdempotencyKey(anyString())).thenReturn(Optional.of(h));
        when(bookings.findById("b1")).thenReturn(Optional.of(b));when(payments.findById("p1")).thenReturn(Optional.of(new Payment()));
        assertThrows(IllegalStateException.class, () -> service.startCheckout(request(),"u1"));
    }
    @Test void paidCancellationMustNotSilentlyDiscardMoney() {
        CourtHold h=existing();h.setStatus("CONFIRMED");
        assertThrows(IllegalStateException.class, () -> service.releaseHold("h1","u1"));
        assertEquals("CONFIRMED",h.getStatus());
    }
    @Test void pendingCancellationReleasesSlots() {
        CourtHold h=existing();service.releaseHold("h1","u1");assertEquals("RELEASED",h.getStatus());
    }
    @Test void verificationWindowHasExclusiveEnd() {
        CourtHold h=existing();
        assertEquals("VERIFYING_PAYMENT",h.getDynamicStatus(h.getPaymentDeadline()));
        assertEquals("EXPIRED",h.getDynamicStatus(h.getVerificationDeadline()));
    }
    @Test void currentMorningSlotsAreNotBookable() {
        var availability=service.getAvailability("c1","f1","15/11/2026");
        assertEquals("UNAVAILABLE",availability.getSlots().get(0).getStatus());
    }
}
