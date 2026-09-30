package com.example.orders;

/** What an order costs, after its discount. */
public final class Pricing {
    /** Ten percent off with this code. */
    static final String TEN_PERCENT_OFF = "SAVE10";

    private static final int PERCENT = 100;
    private static final int TEN = 10;

    private Pricing() {
    }

    /** The order's total, in cents. */
    public static long totalCents(Order order) {
        long subtotal = order.lines().stream().mapToLong(OrderLine::totalCents).sum();
        return subtotal - discountCents(subtotal, order.discountCode());
    }

    private static long discountCents(long subtotal, String code) {
        if (TEN_PERCENT_OFF.equals(code)) {
            return subtotal * TEN / PERCENT;
        }
        return 0;
    }
}
