package com.example.entity;

import java.util.Objects;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    public Category() {
    }

    public Category(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Category))
            return false;
        Category other = (Category) o;
        // If id is null, only same instance is equal (already caught by == above)
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        // Fixed constant: all Category objects get bucket 31.
        // This is "good enough" — it means more hash collisions but is ALWAYS CORRECT.
        // Never use id here if it can be null.
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Category{id=" + id + ", name='" + name + "'}";
    }
}