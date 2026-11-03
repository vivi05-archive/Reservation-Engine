package com.example.inventory.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

// One mutex per item. Different items never block each other.
@Component
public class ItemLocks {

    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T callLocked(Long itemId, Supplier<T> criticalSection) {
        ReentrantLock lock = locks.computeIfAbsent(itemId, id -> new ReentrantLock(true)); // fair = FIFO
        lock.lock();                       // entry section: wait for my turn
        try {
            return criticalSection.get();  // critical section
        } finally {
            lock.unlock();                 // exit section (always, even on exception)
        }
    }
}
