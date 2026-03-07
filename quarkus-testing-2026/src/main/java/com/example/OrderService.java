package com.example;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrderService {

    @Inject
    OrderRepository orderRepository;

    @Inject
    OrderAudit orderAudit;

    @Transactional
    public Order create(String customerId, String product) {
        Order order = new Order(customerId, product);
        orderRepository.persist(order);
        orderAudit.recordCreated(order);
        return order;
    }

    public List<Order> findByCustomer(String customerId) {
        return orderRepository.findByCustomer(customerId);
    }
}