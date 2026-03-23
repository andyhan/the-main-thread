/// In-memory book review registry for tutorials and demos.
///
/// ## Where to start
///
/// - [BookReviewService] is the entry point for callers.
/// - [BookReview] is the immutable result type returned from the service.
/// - [ReviewSubmission] carries the fields passed to [BookReviewService] when creating a review.
///
/// ## Formats
///
/// [BookFormat] is a sealed hierarchy for optional catalog metadata.
///
/// ## Thread safety
///
/// [BookReviewService] is safe for concurrent use. Individual [BookReview]
/// instances are immutable value objects.
package dev.mainthread.bookreviews;
