package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class OrderAuditRedisTest {

    @Inject
    OrderService orderService;

    @Inject
    RedisDataSource redis;

    @Test
    void createOrder_appendsIdToRedisAuditList() {
        ListCommands<String, String> listCommands = redis.list(String.class);
        String key = "orders:created";

        Order order = orderService.create("audit-customer", "desk");

        assertThat(order.id).isNotNull();
        assertThat(listCommands.lrange(key, 0, -1)).contains(String.valueOf(order.id));
    }
}
