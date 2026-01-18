package com.example.entity;

import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "statuses", uniqueConstraints = @UniqueConstraint(columnNames = "statusId"), indexes = {
        @Index(columnList = "instance"),
        @Index(columnList = "createdAt"),
        @Index(columnList = "accountUsername")
})
public class Status extends PanacheEntity {

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(1000)")
    public String statusId;

    @Column(nullable = false, columnDefinition = "VARCHAR(500)")
    public String accountUsername;

    @Column(nullable = false, columnDefinition = "VARCHAR(500)")
    public String instance;

    @Column(columnDefinition = "TEXT")
    public String content;

    @Column(columnDefinition = "TEXT")
    public String url;

    @Column(nullable = false)
    public Instant createdAt;

    @Column(nullable = false)
    public Instant recordedAt;
}
