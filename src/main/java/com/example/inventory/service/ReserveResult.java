package com.example.inventory.service;

import com.example.inventory.entity.Reservation;

public record ReserveResult(boolean success, String message, Reservation reservation, int attempts) {

    public static ReserveResult ok(Reservation reservation, int attempts) {
        return new ReserveResult(true, "Reserved #" + reservation.getId(), reservation, attempts);
    }

    public static ReserveResult rejected(String message, int attempts) {
        return new ReserveResult(false, message, null, attempts);
    }

    public ReserveResult withAttempts(int attempts) {
        return new ReserveResult(success, message, reservation, attempts);
    }
}
