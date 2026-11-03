package com.example.inventory.config;

import com.example.inventory.entity.Item;
import com.example.inventory.repository.ItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ItemRepository items;

    public DataSeeder(ItemRepository items) {
        this.items = items;
    }

    @Override
    public void run(String... args) {          // runs once when the app starts
        if (items.count() > 0) return;
        items.saveAll(List.of(
                new Item("Laptop", 10),
                new Item("Phone", 25),
                new Item("Headphones", 8)));
    }
}
