package com.example.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The application end to end: what it prints. */
class OrdersApplicationIT {
    @Test
    void printsTheDiscountedTotal() {
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        PrintStream standardOut = System.out;
        System.setOut(new PrintStream(printed, true, StandardCharsets.UTF_8));
        try {
            OrdersApplication.main(new String[0]);
        } finally {
            System.setOut(standardOut);
        }

        assertEquals("Total: 810 cents", printed.toString(StandardCharsets.UTF_8).trim());
    }
}
