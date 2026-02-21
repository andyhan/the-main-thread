package com.example.entity;

import java.util.Objects;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String sku; // Business key — assigned at construction, never changes

    public String name;

    @Column(name = "price")
    public java.math.BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    public Tag category;

    // Required for JPA
    public Product() {
    }

    public Product(String sku, String name, java.math.BigDecimal price) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU must not be blank");
        }
        this.sku = sku;
        this.name = name;
        this.price = price;
    }

    /**
     * Equality based on the business key (SKU).
     * Works before AND after persistence.
     * Works with Hibernate proxies (we check instanceof, not getClass()).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        // Use instanceof + null check — works with Hibernate proxies
        if (!(o instanceof Product))
            return false;
        Product other = (Product) o;
        // SKU must never be null (enforced in constructor)
        return Objects.equals(sku, other.sku);
    }

    /**
     * hashCode based ONLY on the business key.
     * This never changes over the entity's lifetime.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(sku);
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", sku='" + sku + "', name='" + name + "'}";
    }
}