package com.example.orders;

import java.util.List;

/** An order: its lines, and the discount code the customer entered, if any. */
public record Order(List<OrderLine> lines, String discountCode) {
    public Order {
        lines = List.copyOf(lines);
    }
}
