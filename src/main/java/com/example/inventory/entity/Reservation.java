package com.example.inventory.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long itemId;
    private String itemName;
    private String customer;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean simulated;                 // true = created by the "simulate users" button

    protected Reservation() { }

    public Reservation(Item item, String customer, int quantity, int holdSeconds, boolean simulated) {
        this.itemId = item.getId();
        this.itemName = item.getName();
        this.customer = customer;
        this.quantity = quantity;
        this.status = ReservationStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusSeconds(holdSeconds);
        this.simulated = simulated;
    }

    public Long getId() { return id; }
    public Long getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getCustomer() { return customer; }
    public int getQuantity() { return quantity; }
    public ReservationStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isSimulated() { return simulated; }
}
