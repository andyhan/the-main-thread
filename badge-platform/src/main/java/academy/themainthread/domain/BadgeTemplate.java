package academy.themainthread.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "badge_template")
public class BadgeTemplate extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String criteria;

    @Column(name = "image_url", nullable = false)
    public String imageUrl;

    @Column
    public String skills;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @JsonIgnore
    @OneToMany(mappedBy = "template", fetch = FetchType.LAZY)
    public List<BadgeAssertion> assertions;

    public List<String> skillList() {
        if (skills == null || skills.isBlank()) {
            return List.of();
        }
        return Arrays.asList(skills.split(","));
    }
}