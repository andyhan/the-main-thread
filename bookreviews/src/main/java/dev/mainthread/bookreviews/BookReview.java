package dev.mainthread.bookreviews;

/// Represents a stored review for a single book.
///
/// Instances are immutable. The `id` is assigned by [BookReviewService] when a
/// review is created. Optional [BookFormat] metadata is for catalog UIs only;
/// the service does not interpret it beyond storage.
///
/// @param id system-assigned identifier
/// @param isbn ISBN-13 string in the form the caller supplied
/// @param title display title of the reviewed book
/// @param reviewer display name of the reviewer
/// @param rating score from `1` to `5`
/// @param body free-text review content
/// @param format optional [BookFormat], or `null` if unknown
public record BookReview(
        Long id,
        String isbn,
        String title,
        String reviewer,
        int rating,
        String body,
        BookFormat format
) {
}
