package com.example;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderRepository implements PanacheRepository<Order> {

    public List<Order> findByCustomer(String customerId) {
        return list("customerId", customerId);
    }
}