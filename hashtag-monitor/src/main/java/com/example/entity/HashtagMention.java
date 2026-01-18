package com.example.entity;

import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "hashtag_mentions", uniqueConstraints = @UniqueConstraint(columnNames = {"status_id", "hashtag"}), indexes = {
        @Index(columnList = "hashtag"),
        @Index(columnList = "recordedAt"),
        @Index(columnList = "status_id")
})
public class HashtagMention extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    public Status status;

    @Column(nullable = false)
    public String hashtag;

    @Column(nullable = false)
    public Instant recordedAt;
}