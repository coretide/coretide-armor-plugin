package com.example.inventory;

import java.util.HashMap;
import java.util.Map;

/** How many of each product are in stock. */
public final class Stock {
    private final Map<String, Integer> counts = new HashMap<>();

    /** Adds [quantity] of [product] to the stock. */
    public void receive(String product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        counts.merge(product, quantity, Integer::sum);
    }

    /** Takes [quantity] of [product] out of stock, if there is enough; says whether there was. */
    public boolean ship(String product, int quantity) {
        int available = available(product);
        if (quantity > available) {
            return false;
        }
        counts.put(product, available - quantity);
        return true;
    }

    public int available(String product) {
        return counts.getOrDefault(product, 0);
    }
}
