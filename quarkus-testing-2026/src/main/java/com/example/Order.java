package com.example;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order extends PanacheEntity {

    @Column(nullable = false)
    public String customerId;

    @Column(nullable = false)
    public String product;

    public Order() {
    }

    public Order(String customerId, String product) {
        this.customerId = customerId;
        this.product = product;
    }
}
