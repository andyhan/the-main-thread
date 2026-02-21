package com.example.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Tag extends PanacheEntity {

    public String name;

    // NAIVE — using all fields including mutable ones
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Tag category = (Tag) o;
        return java.util.Objects.equals(id, category.id) &&
                java.util.Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name); // mutable fields in hash
    }
}