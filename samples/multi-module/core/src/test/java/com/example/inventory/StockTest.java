package com.example.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StockTest {
    @Test
    void addsWhatIsReceived() {
        Stock stock = new Stock();
        stock.receive("bolt", 5);
        stock.receive("bolt", 3);

        assertEquals(8, stock.available("bolt"));
    }

    @Test
    void shipsOnlyWhatIsInStock() {
        Stock stock = new Stock();
        stock.receive("bolt", 5);

        assertTrue(stock.ship("bolt", 5));
        assertFalse(stock.ship("bolt", 1));
        assertEquals(0, stock.available("bolt"));
    }

    @Test
    void refusesToReceiveNothing() {
        assertThrows(IllegalArgumentException.class, () -> new Stock().receive("bolt", 0));
    }
}
