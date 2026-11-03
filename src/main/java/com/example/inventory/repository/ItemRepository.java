package com.example.inventory.repository;

import com.example.inventory.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    boolean existsByName(String name);

    // READ part of CAS: fresh stock + version straight from the database.
    Optional<StockSnapshot> findSnapshotById(Long id);

    // COMPARE-AND-SWAP: one atomic SQL statement. Returns 1 if it won, 0 if someone changed the row first.
    @Modifying
    @Query("update Item i set i.stock = :newStock, i.casVersion = i.casVersion + 1 "
         + "where i.id = :id and i.casVersion = :expectedVersion")
    int compareAndSwap(@Param("id") Long id,
                       @Param("newStock") int newStock,
                       @Param("expectedVersion") long expectedVersion);

    // Atomic "stock = stock + qty" (used for cancel / expiry / restock).
    @Modifying
    @Query("update Item i set i.stock = i.stock + :qty, i.casVersion = i.casVersion + 1 where i.id = :id")
    int addStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("update Item i set i.stock = :stock, i.casVersion = i.casVersion + 1 where i.id = :id")
    int setStock(@Param("id") Long id, @Param("stock") int stock);
}
