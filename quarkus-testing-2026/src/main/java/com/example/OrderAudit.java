package com.example;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.ListCommands;

@ApplicationScoped
public class OrderAudit {

    private static final String LIST_KEY = "orders:created";

    private final ListCommands<String, String> listCommands;

    public OrderAudit(RedisDataSource redis) {
        this.listCommands = redis.list(String.class);
    }

    public void recordCreated(Order order) {
        listCommands.rpush(LIST_KEY, String.valueOf(order.id));
    }
}
