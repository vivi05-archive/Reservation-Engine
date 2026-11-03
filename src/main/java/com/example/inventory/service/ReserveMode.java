package com.example.inventory.service;

public enum ReserveMode {
    UNSAFE,   // no protection: race condition possible
    MUTEX,    // per-item lock around the critical section
    CAS       // lock-free: compare-and-swap + retry
}
