package com.easysport.repository;

import com.easysport.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> lockById(@org.springframework.data.repository.query.Param("id") String id);
    Optional<Payment> findByOrderInvoiceNumber(String orderInvoiceNumber);
    Optional<Payment> findByCheckoutToken(String checkoutToken);
    Optional<Payment> findBySepayTransactionId(String sepayTransactionId);
    List<Payment> findByBookingIdOrderByCreatedAtDesc(String bookingId);
    List<Payment> findByRecurringPackageIdOrderByCreatedAtDesc(String recurringPackageId);
    boolean existsBySepayTransactionId(String sepayTransactionId);
}

