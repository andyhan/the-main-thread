package dev.mainthread.bookreviews;

/// Caller-supplied data used to create a [BookReview].
///
/// ## Preconditions
///
/// The service rejects invalid input with [IllegalArgumentException]:
///
/// - `isbn`, `title`, `reviewer`, and `body` must be non-blank after trimming.
/// - `rating` must be between `1` and `5` inclusive.
/// - `body` length should stay within a reasonable bound for your product; this
///   library uses a soft maximum of `4000` characters after trim.
///
/// ## ISBN
///
/// This type does not parse or checksum ISBNs. Callers should pass normalized
/// strings if their domain requires it.
///
/// @param isbn ISBN-13 or other normalized identifier string
/// @param title non-empty book title
/// @param reviewer non-empty reviewer name
/// @param rating score from `1` to `5`
/// @param body review text
/// @param format optional [BookFormat], may be `null`
public record ReviewSubmission(
        String isbn,
        String title,
        String reviewer,
        int rating,
        String body,
        BookFormat format
) {
}
