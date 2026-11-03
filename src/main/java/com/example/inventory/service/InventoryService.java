package com.example.inventory.service;

import com.example.inventory.entity.Item;
import com.example.inventory.exception.InventoryException;
import com.example.inventory.repository.ItemRepository;
import com.example.inventory.repository.ReservationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
public class InventoryService {

    private final ItemRepository items;
    private final ReservationRepository reservations;
    private final TransactionTemplate tx;
    private final ItemLocks locks;
    private final ActivityLog log;

    public InventoryService(ItemRepository items, ReservationRepository reservations,
                            TransactionTemplate tx, ItemLocks locks, ActivityLog log) {
        this.items = items;
        this.reservations = reservations;
        this.tx = tx;
        this.locks = locks;
        this.log = log;
    }

    public List<Item> findAll() {
        return items.findAll(Sort.by("id"));
    }

    public Item get(Long id) {
        return items.findById(id)
                .orElseThrow(() -> new InventoryException(HttpStatus.NOT_FOUND, "Item " + id + " not found"));
    }

    public Item create(String name, int stock) {
        if (name == null || name.isBlank()) throw new InventoryException(HttpStatus.BAD_REQUEST, "Name is required");
        if (stock < 0) throw new InventoryException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");
        String clean = name.trim();
        if (items.existsByName(clean)) throw new InventoryException(HttpStatus.CONFLICT, "Item already exists");
        Item saved = items.save(new Item(clean, stock));
        log.add("Item added: " + clean + " (stock " + stock + ")");
        return saved;
    }

    public Item restock(Long id, int quantity) {
        if (quantity < 1) throw new InventoryException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        get(id);
        locks.callLocked(id, () -> tx.execute(s -> items.addStock(id, quantity)));
        log.add("Restocked item " + id + " by " + quantity);
        return get(id);
    }

    // Used by the simulation: wipe old simulated reservations and set a known starting stock.
    public void resetForSimulation(Long id, int stock) {
        get(id);
        locks.callLocked(id, () -> tx.execute(s -> {
            reservations.deleteSimulated(id);
            return items.setStock(id, stock);
        }));
    }
}
