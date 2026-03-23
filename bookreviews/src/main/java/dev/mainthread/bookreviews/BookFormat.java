package dev.mainthread.bookreviews;

/// Describes how a title is distributed or held for display.
///
/// This type is closed: only [Paperback] and [Ebook] exist. If you add a new
/// format, update this hierarchy and the package overview in
/// `package-info.java`.
public sealed interface BookFormat permits BookFormat.Paperback, BookFormat.Ebook {

    /// A print copy with rough dimensions for shelving or shipping estimates.
    ///
    /// @param dimensions human-readable size, for example `23 x 15 cm`
    record Paperback(String dimensions) implements BookFormat {
    }

    /// A digital edition identified by a stable download or storefront URL.
    ///
    /// @param uri location of the digital edition
    record Ebook(String uri) implements BookFormat {
    }
}
