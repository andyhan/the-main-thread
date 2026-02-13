package com.example;

import java.util.List;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.quarkus.hibernate.panache.WithId;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
public class Book extends WithId.AutoLong {

    @Column(nullable = false)
    public String title;

    public int year;

    @ManyToOne(fetch = FetchType.EAGER) // must be EAGER in stateless mode
    @JoinColumn(name = "author_id")
    public Author author;

    public interface Repo
            extends PanacheRepository.Reactive.Stateless<Book, Long> {

        @Find
        Uni<List<Book>> findByTitle(String title);

        @HQL("where year >= :year order by year desc")
        Uni<List<Book>> findPublishedSince(int year);

        @HQL("delete from Book where year < :year")
        Uni<Integer> deleteOlderThan(int year);
    }
}