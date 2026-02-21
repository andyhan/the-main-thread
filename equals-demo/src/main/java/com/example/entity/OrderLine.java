package com.example.entity;

import java.util.Objects;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_lines")
public class OrderLine extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Long id;

    // UUID assigned at construction — stable across the entire lifecycle
    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    public final UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    public Product product;

    @Column(nullable = false)
    public int quantity;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OrderLine))
            return false;
        OrderLine other = (OrderLine) o;
        return Objects.equals(uuid, other.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }

    @Override
    public String toString() {
        return "OrderLine{id=" + id + ", uuid=" + uuid + ", quantity=" + quantity + "}";
    }
}