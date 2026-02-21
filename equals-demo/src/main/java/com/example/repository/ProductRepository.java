package com.example.repository;

import com.example.entity.Product;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {
    public Product findBySku(String sku) {
        return find("sku", sku).firstResult();
    }
}
