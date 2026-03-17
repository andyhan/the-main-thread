package dev.mainthread;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/bookstore")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class BookstoreResource {

    @Inject
    EntityManager em;

    /**
     * N+1 endpoint (deliberately broken) that demonstrates the N+1 problem.
     * <p>
     * Returns a list of authors with their book counts.
     * <p>
     * Loads all authors in one query, then issues one query per author to fetch
     * their books. Classic N+1.
     */

    @GET
    @Path("/authors-with-books-n1")
    @Transactional
    public List<Map<String, Object>> authorsWithBooksN1() {
        // Loads all authors in one query, then issues one query per author
        // to fetch their books. Classic N+1.
        List<Author> authors = em.createQuery("FROM Author a", Author.class)
                .getResultList();

        return authors.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("author", a.name);
            m.put("bookCount", a.books.size()); // <-- triggers lazy load per author
            return m;
        }).toList();
    }

    /**
     * Fixed endpoint (JOIN FETCH) that fixes the N+1 problem.
     * <p>
     * Returns a list of authors with their book counts.
     * <p>
     * Uses a JOIN FETCH to load the books in a single query, avoiding the N+1
     * problem.
     */

    @GET
    @Path("/authors-with-books")
    @Transactional
    public List<Map<String, Object>> authorsWithBooks() {
        List<Author> authors = em.createQuery(
                "SELECT DISTINCT a FROM Author a JOIN FETCH a.books", Author.class)
                .getResultList();

        return authors.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("author", a.name);
            m.put("bookCount", a.books.size());
            return m;
        }).toList();
    }

    private Statistics stats() {
        return em.getEntityManagerFactory()
                 .unwrap(SessionFactory.class)
                 .getStatistics();
    }

    @GET
    @Path("/stats")
    public Map<String, Object> statsEndpoint() {
        Statistics s = stats();
        return Map.of(
                "queriesExecuted", s.getQueryExecutionCount(),
                "entitiesLoaded", s.getEntityLoadCount(),
                "collectionsFetched", s.getCollectionFetchCount(),
                "preparedStatements", s.getPrepareStatementCount(),
                "slowestQuery", s.getQueryExecutionMaxTimeQueryString() != null
                        ? s.getQueryExecutionMaxTimeQueryString()
                        : "none recorded",
                "slowestQueryMs", s.getQueryExecutionMaxTime());
    }

    @POST
    @Path("/stats/reset")
    public void resetStats() {
        stats().clear();
    }
}