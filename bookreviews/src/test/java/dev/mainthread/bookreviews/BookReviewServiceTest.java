package dev.mainthread.bookreviews;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookReviewServiceTest {

    @Test
    void createFindAndDeleteRoundTrip() {
        var service = new BookReviewService();
        var submission = new ReviewSubmission(
                "9780134685991",
                "Effective Java",
                "mjava",
                5,
                "Essential reading for any Java developer.",
                new BookFormat.Paperback("23 x 15 cm")
        );

        BookReview created = service.create(submission);
        assertEquals("Effective Java", created.title());
        assertEquals(5, created.rating());

        BookReview found = service.findById(created.id());
        assertEquals(created, found);

        service.delete(created.id());
        assertThrows(ReviewNotFoundException.class, () -> service.findById(created.id()));
    }

    @Test
    void rejectsOutOfRangeRating() {
        var service = new BookReviewService();
        var bad = new ReviewSubmission(
                "9780134685991",
                "Effective Java",
                "mjava",
                9,
                "Essential reading for any Java developer.",
                null
        );
        assertThrows(IllegalArgumentException.class, () -> service.create(bad));
    }

    @Test
    void notFoundIsStable() {
        var service = new BookReviewService();
        var ex = assertThrows(ReviewNotFoundException.class, () -> service.findById(999L));
        assertEquals("No review found with id 999", ex.getMessage());
    }
}
