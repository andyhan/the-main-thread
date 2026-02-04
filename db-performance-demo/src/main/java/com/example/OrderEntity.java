package com.example;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class OrderEntity extends PanacheEntity {

    public String customerId;

    public double amount;
}