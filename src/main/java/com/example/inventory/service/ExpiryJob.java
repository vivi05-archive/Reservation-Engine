package com.example.inventory.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiryJob {

    private final ReservationService reservationService;

    public ExpiryJob(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelay = 2000)             // run, wait 2 s after finishing, run again
    public void sweep() {
        reservationService.expireOverdue();
    }
}
