package dev.mainthread;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@QuarkusTest
class QueryCountTest {

    @Inject
    EntityManager em;

    private Statistics stats() {
        return em.getEntityManagerFactory()
                 .unwrap(SessionFactory.class)
                 .getStatistics();
    }

    @BeforeEach
    void reset() {
        stats().clear();
    }

    @Test
    @Transactional
    void joinFetchIssuesExactlyOneQuery() {
        em.createQuery(
                "SELECT DISTINCT a FROM Author a JOIN FETCH a.books", Author.class)
          .getResultList()
          .forEach(a -> a.books.size()); // force access to collections

        assertThat(stats().getQueryExecutionCount())
            .as("JOIN FETCH should require exactly 1 query")
            .isEqualTo(1);

        assertThat(stats().getCollectionFetchCount())
            .as("No additional collection fetches should occur with JOIN FETCH")
            .isEqualTo(0);
    }

    @Test
    @Transactional
    void lazyLoadingProducesNPlusOneStatements() {
        var authors = em.createQuery("FROM Author a", Author.class)
                        .getResultList();

        authors.forEach(a -> a.books.size());

        long authorCount = authors.size();

        assertThat(stats().getQueryExecutionCount())
            .as("Only the HQL query counts as a 'query executed'")
            .isEqualTo(1);

        assertThat(stats().getPrepareStatementCount())
            .as("N+1: 1 (authors query) + N (one lazy load per author)")
            .isEqualTo(1 + authorCount);

        assertThat(stats().getCollectionFetchCount())
            .as("Each author's collection was fetched separately")
            .isEqualTo(authorCount);
    }
}
