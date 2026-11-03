package com.example.inventory.service;

import com.example.inventory.entity.Item;
import com.example.inventory.entity.Reservation;
import com.example.inventory.entity.ReservationStatus;
import com.example.inventory.exception.InventoryException;
import com.example.inventory.repository.ItemRepository;
import com.example.inventory.repository.ReservationRepository;
import com.example.inventory.repository.StockSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {

    public static final int HOLD_SECONDS = 60;
    private static final int MAX_CAS_RETRIES = 200;

    private final ItemRepository items;
    private final ReservationRepository reservations;
    private final TransactionTemplate tx;
    private final ItemLocks locks;
    private final ActivityLog log;
    private final long delayMs;

    public ReservationService(ItemRepository items, ReservationRepository reservations,
                              TransactionTemplate tx, ItemLocks locks, ActivityLog log,
                              @Value("${inventory.processing-delay-ms:5}") long delayMs) {
        this.items = items;
        this.reservations = reservations;
        this.tx = tx;
        this.locks = locks;
        this.log = log;
        this.delayMs = delayMs;
    }

    // ================= RESERVE =================

    public ReserveResult reserve(Long itemId, int qty, String customer, ReserveMode mode, boolean simulated) {
        if (qty < 1) throw new InventoryException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        if (customer == null || customer.isBlank())
            throw new InventoryException(HttpStatus.BAD_REQUEST, "Customer name is required");

        ReserveMode m = (mode == null) ? ReserveMode.MUTEX : mode;
        ReserveResult result = switch (m) {
            case UNSAFE -> checkThenAct(itemId, qty, customer, simulated);
            case MUTEX -> locks.callLocked(itemId, () -> checkThenAct(itemId, qty, customer, simulated));
            case CAS -> reserveWithCas(itemId, qty, customer, simulated);
        };

        log.add(m + " | " + customer + " wants " + qty + " of item " + itemId + " -> " + result.message()
                + (m == ReserveMode.CAS ? " (attempts: " + result.attempts() + ")" : ""));
        return result;
    }

    // The classic CHECK-THEN-ACT. Without a lock around it, this is the race condition.
    private ReserveResult checkThenAct(Long itemId, int qty, String customer, boolean simulated) {
        return tx.execute(status -> {
            Item item = findItem(itemId);
            if (item.getStock() < qty) {                                   // CHECK
                return ReserveResult.rejected("Not enough stock (" + item.getStock() + " left)", 1);
            }
            pause();                                                       // widens the race window
            item.changeStock(-qty);                                        // ACT (written back at commit)
            Reservation saved = reservations.save(new Reservation(item, customer, qty, HOLD_SECONDS, simulated));
            return ReserveResult.ok(saved, 1);
        });
    }

    // Lock-free: read -> compute -> compare-and-swap; if someone else got there first, retry.
    private ReserveResult reserveWithCas(Long itemId, int qty, String customer, boolean simulated) {
        for (int attempt = 1; attempt <= MAX_CAS_RETRIES; attempt++) {
            ReserveResult result = tx.execute(status -> {
                StockSnapshot snap = items.findSnapshotById(itemId)
                        .orElseThrow(() -> new InventoryException(HttpStatus.NOT_FOUND, "Item " + itemId + " not found"));
                if (snap.getStock() < qty) {
                    return ReserveResult.rejected("Not enough stock (" + snap.getStock() + " left)", 1);
                }
                pause();
                int won = items.compareAndSwap(itemId, snap.getStock() - qty, snap.getCasVersion());
                if (won == 0) return null;                                 // lost the race -> retry
                Item item = findItem(itemId);
                Reservation saved = reservations.save(new Reservation(item, customer, qty, HOLD_SECONDS, simulated));
                return ReserveResult.ok(saved, 1);
            });
            if (result != null) return result.withAttempts(attempt);
        }
        return ReserveResult.rejected("Too much contention, please retry", MAX_CAS_RETRIES);
    }

    // ================= CONFIRM / CANCEL / EXPIRE =================

    public Reservation confirm(Long id) {
        Reservation r = findReservation(id);
        if (r.getStatus() == ReservationStatus.ACTIVE && r.getExpiresAt().isBefore(LocalDateTime.now())) {
            release(id, ReservationStatus.EXPIRED);
            throw new InventoryException(HttpStatus.CONFLICT, "Reservation #" + id + " has already expired");
        }
        Integer changed = tx.execute(s -> reservations.transition(id, ReservationStatus.ACTIVE, ReservationStatus.CONFIRMED));
        if (changed == null || changed == 0) {
            throw new InventoryException(HttpStatus.CONFLICT, "Reservation #" + id + " is no longer ACTIVE");
        }
        log.add("Reservation #" + id + " confirmed");
        return findReservation(id);
    }

    public Reservation cancel(Long id) {
        findReservation(id);                                              // 404 if it does not exist
        if (!release(id, ReservationStatus.CANCELLED)) {
            throw new InventoryException(HttpStatus.CONFLICT, "Reservation #" + id + " is no longer ACTIVE");
        }
        log.add("Reservation #" + id + " cancelled, stock returned");
        return findReservation(id);
    }

    // Called by ExpiryJob every 2 seconds.
    public void expireOverdue() {
        List<Reservation> overdue = reservations
                .findByStatusAndSimulatedFalseAndExpiresAtBefore(ReservationStatus.ACTIVE, LocalDateTime.now());
        for (Reservation r : overdue) {
            if (release(r.getId(), ReservationStatus.EXPIRED)) {
                log.add("Reservation #" + r.getId() + " expired, " + r.getQuantity() + " x " + r.getItemName() + " returned");
            }
        }
    }

    // ACTIVE -> CANCELLED/EXPIRED and give the stock back. Returns false if someone else already finished it.
    private boolean release(Long id, ReservationStatus newStatus) {
        Reservation r = findReservation(id);
        Boolean released = locks.callLocked(r.getItemId(), () -> tx.execute(status -> {
            int changed = reservations.transition(id, ReservationStatus.ACTIVE, newStatus);
            if (changed == 0) return false;
            items.addStock(r.getItemId(), r.getQuantity());
            return true;
        }));
        return Boolean.TRUE.equals(released);
    }

    // ================= QUERIES / HELPERS =================

    public List<Reservation> list() {
        return reservations.findBySimulatedFalseOrderByIdDesc();
    }

    private Item findItem(Long id) {
        return items.findById(id)
                .orElseThrow(() -> new InventoryException(HttpStatus.NOT_FOUND, "Item " + id + " not found"));
    }

    private Reservation findReservation(Long id) {
        return reservations.findById(id)
                .orElseThrow(() -> new InventoryException(HttpStatus.NOT_FOUND, "Reservation " + id + " not found"));
    }

    private void pause() {
        if (delayMs <= 0) return;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
