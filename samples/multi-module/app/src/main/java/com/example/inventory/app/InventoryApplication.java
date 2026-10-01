package com.example.inventory.app;

import com.example.inventory.Stock;

/** Ships from a small stock. Coverage leaves it out: CodeArmor excludes entry points named *Application. */
public final class InventoryApplication {
    private InventoryApplication() {
    }

    public static void main(String[] args) {
        Stock stock = new Stock();
        stock.receive("bolt", 10);
        System.out.println("Short by " + Restock.shipOrShortfall(stock, "bolt", 12));
    }
}
