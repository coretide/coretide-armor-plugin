package com.example.orders;

/** A product, how many of it, and its price in cents. */
public record OrderLine(String product, int quantity, long unitPriceCents) {
    public OrderLine {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        if (unitPriceCents < 0) {
            throw new IllegalArgumentException("price must not be negative: " + unitPriceCents);
        }
    }

    long totalCents() {
        return quantity * unitPriceCents;
    }
}
