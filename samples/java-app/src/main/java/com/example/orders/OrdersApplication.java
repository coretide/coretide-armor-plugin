package com.example.orders;

import java.util.List;

/** Prices a sample order. Coverage leaves it out: CodeArmor excludes entry points named *Application. */
public final class OrdersApplication {
    private OrdersApplication() {
    }

    public static void main(String[] args) {
        Order order = new Order(List.of(new OrderLine("coffee", 2, 450)), Pricing.TEN_PERCENT_OFF);
        System.out.println("Total: " + Pricing.totalCents(order) + " cents");
    }
}
