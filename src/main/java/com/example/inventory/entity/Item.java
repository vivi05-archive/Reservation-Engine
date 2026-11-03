package com.example.inventory.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private int stock;

    // Bumped on EVERY stock change. The CAS strategy uses it to detect "someone changed it".
    private long casVersion;

    protected Item() { }                       // required by JPA

    public Item(String name, int stock) {
        this.name = name;
        this.stock = stock;
    }

    public void changeStock(int delta) {
        this.stock += delta;
        this.casVersion++;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getStock() { return stock; }
    public long getCasVersion() { return casVersion; }
}
