package com.example.inventory.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.inventory.Stock;
import org.junit.jupiter.api.Test;

/** Also covers Stock.ship in the core module, which only the combined coverage report counts. */
class RestockTest {
    @Test
    void shipsWhatIsInStock() {
        Stock stock = new Stock();
        stock.receive("bolt", 10);

        assertEquals(0, Restock.shipOrShortfall(stock, "bolt", 4));
        assertEquals(6, stock.available("bolt"));
    }

    @Test
    void saysHowManyAreMissing() {
        Stock stock = new Stock();
        stock.receive("bolt", 10);

        assertEquals(2, Restock.shipOrShortfall(stock, "bolt", 12));
        assertEquals(10, stock.available("bolt"));
    }
}
