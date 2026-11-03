package com.example.inventory.repository;

import com.example.inventory.entity.Reservation;
import com.example.inventory.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findBySimulatedFalseOrderByIdDesc();

    List<Reservation> findByStatusAndSimulatedFalseAndExpiresAtBefore(ReservationStatus status, LocalDateTime time);

    // Atomic status change: only succeeds if the reservation is still in the expected status.
    // This is what stops "cancel" and "expire" from both returning the stock.
    @Modifying
    @Query("update Reservation r set r.status = :newStatus where r.id = :id and r.status = :expected")
    int transition(@Param("id") Long id,
                   @Param("expected") ReservationStatus expected,
                   @Param("newStatus") ReservationStatus newStatus);

    @Modifying
    @Query("delete from Reservation r where r.itemId = :itemId and r.simulated = true")
    int deleteSimulated(@Param("itemId") Long itemId);
}
