package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class OrderServiceTest {

    @Inject
    OrderService orderService;

    @Test
    void persistAndReloadOrder() {
        orderService.create("customer-1", "laptop");

        List<Order> orders = orderService.findByCustomer("customer-1");

        assertThat(orders).hasSize(1);
    }
}