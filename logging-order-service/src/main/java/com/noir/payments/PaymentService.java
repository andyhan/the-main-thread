package com.noir.payments;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PaymentService {

    public void charge(String orderId, double amount) {

        Log.infof("Initiating payment: orderId=%s amount=%.2f",
                orderId, amount);

        if (amount > 10000) {
            Log.warnf("Large transaction flagged: orderId=%s amount=%.2f",
                    orderId, amount);
        }

        if (amount < 0) {
            Log.errorf("Invalid amount: orderId=%s", orderId);
            throw new IllegalArgumentException("Amount must be positive");
        }

        Log.infof("Payment authorised: orderId=%s", orderId);
    }
}