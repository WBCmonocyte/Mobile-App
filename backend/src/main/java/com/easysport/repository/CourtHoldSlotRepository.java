package com.easysport.repository;

import com.easysport.entity.CourtHoldSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CourtHoldSlotRepository extends JpaRepository<CourtHoldSlot, String> {

    List<CourtHoldSlot> findByHoldId(String holdId);

    /**
     * Tìm kiếm các slot đang xung đột:
     * Cùng tài nguyên, cùng ngày, trạng thái BOOKED/CONFIRMED hoặc HELD/AWAITING_PAYMENT/VERIFYING_PAYMENT chưa hết hạn (expiresAt > now),
     * và khoảng thời gian chồng lấn: startA < endB AND startB < endA.
     */
    @Query("SELECT s FROM CourtHoldSlot s WHERE s.resourceId IN :resourceIds " +
           "AND s.dateLabel = :dateLabel " +
           "AND (s.status IN ('BOOKED', 'CONFIRMED') OR (s.status IN ('HELD', 'AWAITING_PAYMENT', 'VERIFYING_PAYMENT') AND s.expiresAt > :now)) " +
           "AND (:startMinute < s.endMinute AND s.startMinute < :endMinute)")
    List<CourtHoldSlot> findOverlappingActiveSlots(
            @Param("resourceIds") List<String> resourceIds,
            @Param("dateLabel") String dateLabel,
            @Param("startMinute") int startMinute,
            @Param("endMinute") int endMinute,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT s FROM CourtHoldSlot s WHERE s.courtId = :courtId " +
           "AND s.dateLabel = :dateLabel " +
           "AND (s.status IN ('BOOKED', 'CONFIRMED') OR (s.status IN ('HELD', 'AWAITING_PAYMENT', 'VERIFYING_PAYMENT') AND s.expiresAt > :now))")
    List<CourtHoldSlot> findActiveSlotsForCourt(
            @Param("courtId") String courtId,
            @Param("dateLabel") String dateLabel,
            @Param("now") LocalDateTime now
    );
}
