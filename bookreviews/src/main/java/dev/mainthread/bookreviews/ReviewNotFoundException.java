package dev.mainthread.bookreviews;

/// Thrown when a requested [BookReview] does not exist in the registry.
///
/// Callers that map errors to user-visible messages can rely on
/// [getMessage] for a stable English sentence in this implementation.
public class ReviewNotFoundException extends RuntimeException {

    /// @param id identifier that was not found
    public ReviewNotFoundException(Long id) {
        super("No review found with id " + id);
    }
}
