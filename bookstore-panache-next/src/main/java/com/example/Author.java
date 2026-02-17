package com.example;

import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "authors")
public class Author extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    public String country;

    // Blocking managed — one repo for all blocking operations
    public interface Repo extends PanacheRepository<Author> {

        // Type-safe: Hibernate validates 'name' exists on Author at build time
        @Find
        Optional<Author> findByName(String name);

        // HQL string still works for complex queries
        @HQL("where country = :country order by name")
        List<Author> findByCountry(String country);

        // Build-time-validated delete
        @HQL("delete from Author where country = :country")
        long deleteByCountry(String country);
    }

    // Reactive stateless — for read-heavy endpoints
    public interface ReadRepo
            extends PanacheRepository.Reactive.Stateless<Author, Long> {
        @HQL("where country = :c order by name")
        Uni<List<Author>> catalog(String c);
    }
}
