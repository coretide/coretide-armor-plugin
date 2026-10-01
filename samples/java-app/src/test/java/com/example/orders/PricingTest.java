package com.example.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class PricingTest {
    @Test
    void addsUpTheLines() {
        Order order = new Order(List.of(new OrderLine("tea", 3, 300), new OrderLine("cake", 1, 550)), null);

        assertEquals(1450, Pricing.totalCents(order));
    }

    @Test
    void takesTenPercentOffWithTheCode() {
        Order order = new Order(List.of(new OrderLine("tea", 2, 500)), Pricing.TEN_PERCENT_OFF);

        assertEquals(900, Pricing.totalCents(order));
    }

    @Test
    void ignoresAnUnknownCode() {
        Order order = new Order(List.of(new OrderLine("tea", 2, 500)), "FREE");

        assertEquals(1000, Pricing.totalCents(order));
    }

    @Test
    void refusesAnEmptyOrNegativeLine() {
        assertThrows(IllegalArgumentException.class, () -> new OrderLine("tea", 0, 500));
        assertThrows(IllegalArgumentException.class, () -> new OrderLine("tea", 1, -1));
    }
}
