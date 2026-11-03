package com.example.inventory.entity;

public enum ReservationStatus {
    ACTIVE,       // stock is held, waiting for confirm / cancel / expiry
    CONFIRMED,    // purchase completed, stock stays deducted
    CANCELLED,    // user cancelled, stock returned
    EXPIRED       // 60 s passed, stock returned automatically
}
