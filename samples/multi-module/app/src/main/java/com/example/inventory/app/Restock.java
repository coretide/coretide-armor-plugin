package com.example.inventory.app;

import com.example.inventory.Stock;

/** Fills an order from stock, or says how many are missing. */
public final class Restock {
    private Restock() {
    }

    /** How many of [product] must be ordered in before [quantity] can ship; 0 when it shipped. */
    public static int shipOrShortfall(Stock stock, String product, int quantity) {
        if (stock.ship(product, quantity)) {
            return 0;
        }
        return quantity - stock.available(product);
    }
}
