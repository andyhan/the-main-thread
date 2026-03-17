package dev.mainthread;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "author")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "author_seq")
    @SequenceGenerator(name = "author_seq", sequenceName = "author_seq", allocationSize = 50)
    public Long id;

    @Column(nullable = false)
    public String name;

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    public List<Book> books;

    public Author() {
    }

    public Author(String name) {
        this.name = name;
    }
}