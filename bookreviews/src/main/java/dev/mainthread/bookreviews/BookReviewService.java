package dev.mainthread.bookreviews;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/// In-memory registry of [BookReview] instances.
///
/// Reviews are stored in a `ConcurrentHashMap` and identified by a monotonically
/// increasing `long` ID.
///
/// ## Thread safety
///
/// Read and write operations are safe for concurrent access. The store itself is
/// thread-safe, and the ID sequence is managed with `AtomicLong`.
///
/// ## Limits
///
/// This implementation does **not** persist data. Discarding the service instance
/// clears all reviews. It is suitable for demos, tests, and embedding in larger
/// applications that supply their own persistence.
///
/// ## Example
///
/// ```java
/// var service = new BookReviewService();
/// BookReview created = service.create(new ReviewSubmission(
///         "9780134685991",
///         "Effective Java",
///         "mjava",
///         5,
///         "Essential reading for any Java developer.",
///         new BookFormat.Paperback("23 x 15 cm")
/// ));
/// BookReview found = service.findById(created.id());
/// ```
public class BookReviewService {

    private static final int BODY_MAX_LEN = 4000;

    private final Map<Long, BookReview> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    /// Creates a new review and assigns a unique ID.
    ///
    /// @param submission validated caller input; see [ReviewSubmission]
    /// @return newly created [BookReview]
    /// @throws IllegalArgumentException if preconditions on [ReviewSubmission] fail
    public BookReview create(ReviewSubmission submission) {
        validateSubmission(submission);
        long id = sequence.getAndIncrement();
        BookReview review = new BookReview(
                id,
                submission.isbn().trim(),
                submission.title().trim(),
                submission.reviewer().trim(),
                submission.rating(),
                submission.body().trim(),
                submission.format()
        );
        store.put(id, review);
        return review;
    }

    /// @param id review identifier
    /// @return matching [BookReview]
    /// @throws ReviewNotFoundException if the ID does not exist
    public BookReview findById(Long id) {
        return Optional.ofNullable(store.get(id))
                .orElseThrow(() -> new ReviewNotFoundException(id));
    }

    /// @return snapshot list of all reviews; not backed by the live store
    public List<BookReview> findAll() {
        return new ArrayList<>(store.values());
    }

    /// @param isbn ISBN string to match exactly against stored reviews
    /// @return matching reviews, possibly empty; does not throw [ReviewNotFoundException]
    public List<BookReview> findByIsbn(String isbn) {
        return store.values().stream()
                .filter(review -> review.isbn().equals(isbn))
                .toList();
    }

    /// @param id review identifier
    /// @throws ReviewNotFoundException if the ID does not exist
    public void delete(Long id) {
        if (store.remove(id) == null) {
            throw new ReviewNotFoundException(id);
        }
    }

    private static void validateSubmission(ReviewSubmission s) {
        if (s.isbn().isBlank() || s.title().isBlank() || s.reviewer().isBlank() || s.body().isBlank()) {
            throw new IllegalArgumentException("isbn, title, reviewer, and body must be non-blank");
        }
        if (s.rating() < 1 || s.rating() > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
        if (s.body().trim().length() > BODY_MAX_LEN) {
            throw new IllegalArgumentException("body exceeds maximum length");
        }
    }
}
