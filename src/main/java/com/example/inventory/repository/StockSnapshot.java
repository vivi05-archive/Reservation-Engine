package com.example.inventory.repository;

// Projection: lets a query return only these two columns instead of a whole Item.
public interface StockSnapshot {
    int getStock();
    long getCasVersion();
}
