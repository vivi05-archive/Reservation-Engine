package com.example.inventory.controller;

import com.example.inventory.dto.Dtos.*;
import com.example.inventory.exception.InventoryException;
import com.example.inventory.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class InventoryController {

    private final InventoryService inventory;
    private final ReservationService reservations;
    private final SimulationService simulation;
    private final ActivityLog log;

    public InventoryController(InventoryService inventory, ReservationService reservations,
                               SimulationService simulation, ActivityLog log) {
        this.inventory = inventory;
        this.reservations = reservations;
        this.simulation = simulation;
        this.log = log;
    }

    // ---------- items ----------
    @GetMapping("/items")
    public List<ItemView> items() {
        return inventory.findAll().stream().map(ItemView::from).toList();
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemView addItem(@RequestBody ItemRequest req) {
        return ItemView.from(inventory.create(req.name(), req.stock()));
    }

    @PostMapping("/items/{id}/restock")
    public ItemView restock(@PathVariable("id") Long id, @RequestBody RestockRequest req) {
        return ItemView.from(inventory.restock(id, req.quantity()));
    }

    // ---------- reservations ----------
    @GetMapping("/reservations")
    public List<ReservationView> reservations() {
        return reservations.list().stream().map(ReservationView::from).toList();
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationView reserve(@RequestBody ReserveRequest req) {
        ReserveResult result = reservations.reserve(req.itemId(), req.quantity(), req.customer(), req.mode(), false);
        if (!result.success()) {
            throw new InventoryException(HttpStatus.CONFLICT, result.message());
        }
        return ReservationView.from(result.reservation());
    }

    @PostMapping("/reservations/{id}/confirm")
    public ReservationView confirm(@PathVariable("id") Long id) {
        return ReservationView.from(reservations.confirm(id));
    }

    @PostMapping("/reservations/{id}/cancel")
    public ReservationView cancel(@PathVariable("id") Long id) {
        return ReservationView.from(reservations.cancel(id));
    }

    // ---------- demo ----------
    @PostMapping("/simulate")
    public SimulationReport simulate(@RequestBody SimulationRequest req) {
        return simulation.run(req);
    }

    @GetMapping("/log")
    public List<String> log() {
        return log.latest(150);
    }

    @DeleteMapping("/log")
    public void clearLog() {
        log.clear();
    }
}
