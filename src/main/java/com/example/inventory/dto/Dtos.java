package com.example.inventory.dto;

import com.example.inventory.entity.Item;
import com.example.inventory.entity.Reservation;
import com.example.inventory.entity.ReservationStatus;
import com.example.inventory.service.ReserveMode;

import java.time.Duration;
import java.time.LocalDateTime;

// All request/response shapes in one file. Records = immutable data carriers.
public final class Dtos {

    private Dtos() { }

    // ----- requests -----
    public record ItemRequest(String name, int stock) { }

    public record RestockRequest(int quantity) { }

    public record ReserveRequest(Long itemId, int quantity, String customer, ReserveMode mode) { }

    public record SimulationRequest(Long itemId, int users, int quantity, int stock, ReserveMode mode) { }

    // ----- responses -----
    public record ItemView(Long id, String name, int stock) {
        public static ItemView from(Item item) {
            return new ItemView(item.getId(), item.getName(), item.getStock());
        }
    }

    public record ReservationView(Long id, Long itemId, String itemName, String customer, int quantity,
                                  ReservationStatus status, long secondsLeft, String createdAt) {
        public static ReservationView from(Reservation r) {
            long left = 0;
            if (r.getStatus() == ReservationStatus.ACTIVE) {
                left = Math.max(0, Duration.between(LocalDateTime.now(), r.getExpiresAt()).getSeconds());
            }
            return new ReservationView(r.getId(), r.getItemId(), r.getItemName(), r.getCustomer(),
                    r.getQuantity(), r.getStatus(), left, r.getCreatedAt().toLocalTime().withNano(0).toString());
        }
    }

    public record SimulationReport(ReserveMode mode, int users, int quantityPerUser, int initialStock,
                                   int successful, int rejected, int errors, int unitsSold, int oversold,
                                   int finalStock, int expectedStock, int totalAttempts, long durationMs,
                                   boolean consistent, String verdict) { }
}
