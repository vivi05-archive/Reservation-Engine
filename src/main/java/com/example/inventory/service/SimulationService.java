package com.example.inventory.service;

import com.example.inventory.dto.Dtos.SimulationReport;
import com.example.inventory.dto.Dtos.SimulationRequest;
import com.example.inventory.entity.Item;
import com.example.inventory.exception.InventoryException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SimulationService {

    private final ReservationService reservationService;
    private final InventoryService inventory;
    private final ActivityLog log;

    public SimulationService(ReservationService reservationService, InventoryService inventory, ActivityLog log) {
        this.reservationService = reservationService;
        this.inventory = inventory;
        this.log = log;
    }

    public SimulationReport run(SimulationRequest req) {
        if (req.itemId() == null) throw new InventoryException(HttpStatus.BAD_REQUEST, "Choose an item");
        if (req.users() < 1 || req.users() > 200)
            throw new InventoryException(HttpStatus.BAD_REQUEST, "Users must be between 1 and 200");
        if (req.quantity() < 1) throw new InventoryException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        if (req.stock() < 0 || req.stock() > 100000)
            throw new InventoryException(HttpStatus.BAD_REQUEST, "Stock must be between 0 and 100000");

        final Long itemId = req.itemId();
        final int users = req.users();
        final int qty = req.quantity();
        final int stock = req.stock();
        final ReserveMode mode = (req.mode() == null) ? ReserveMode.MUTEX : req.mode();

        Item item = inventory.get(itemId);
        inventory.resetForSimulation(itemId, stock);
        log.add(">>> SIMULATION: " + users + " users x" + qty + " of " + item.getName()
                + ", stock " + stock + ", mode " + mode);

        AtomicInteger threadNo = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(users,
                r -> new Thread(r, "user-thread-" + threadNo.incrementAndGet()));
        CountDownLatch ready = new CountDownLatch(users);   // everyone is at the starting line
        CountDownLatch go = new CountDownLatch(1);          // the starting gun

        List<Future<ReserveResult>> futures = new ArrayList<>();
        for (int i = 1; i <= users; i++) {
            final String customer = "sim-user-" + i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return reservationService.reserve(itemId, qty, customer, mode, true);
            }));
        }

        int successful = 0, rejected = 0, errors = 0, attempts = 0;
        long startNs = System.nanoTime();
        try {
            ready.await();
            startNs = System.nanoTime();
            go.countDown();                                  // all threads fire at the same moment
            for (Future<ReserveResult> f : futures) {
                try {
                    ReserveResult r = f.get(60, TimeUnit.SECONDS);
                    attempts += r.attempts();
                    if (r.success()) successful++; else rejected++;
                } catch (ExecutionException | TimeoutException e) {
                    errors++;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InventoryException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulation interrupted");
        } finally {
            pool.shutdownNow();
        }
        long durationMs = (System.nanoTime() - startNs) / 1_000_000;

        int sold = successful * qty;
        int expected = stock - sold;
        int oversold = Math.max(0, sold - stock);
        int finalStock = inventory.get(itemId).getStock();
        boolean consistent = oversold == 0 && finalStock == expected && finalStock >= 0;

        String verdict;
        if (consistent) {
            verdict = "CORRECT: " + sold + " of " + stock + " units sold and the stock matches.";
        } else if (oversold > 0) {
            verdict = "RACE CONDITION: " + oversold + " unit(s) sold that did not exist (overselling).";
        } else {
            verdict = "RACE CONDITION: stock shows " + finalStock + " but should be " + expected + " (lost update).";
        }
        log.add("<<< RESULT: " + verdict);

        return new SimulationReport(mode, users, qty, stock, successful, rejected, errors, sold, oversold,
                finalStock, expected, attempts, durationMs, consistent, verdict);
    }
}
