package com.example.inventory.service;

import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class ActivityLog {

    private static final int MAX_LINES = 300;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final ConcurrentLinkedDeque<String> lines = new ConcurrentLinkedDeque<>();

    public void add(String message) {
        String thread = Thread.currentThread().getName();
        lines.addFirst(LocalTime.now().format(TIME) + " [" + thread + "] " + message);
        while (lines.size() > MAX_LINES) {
            lines.pollLast();
        }
    }

    public List<String> latest(int count) {
        return lines.stream().limit(count).toList();
    }

    public void clear() {
        lines.clear();
    }
}
