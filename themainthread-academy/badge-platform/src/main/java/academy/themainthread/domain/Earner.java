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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "earner")
public class Earner extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(nullable = false)
    public String name;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @JsonIgnore
    @OneToMany(mappedBy = "earner", fetch = FetchType.LAZY)
    public List<BadgeAssertion> assertions;

    public static Earner findByEmail(String email) {
        return find("email", email).firstResult();
    }
}