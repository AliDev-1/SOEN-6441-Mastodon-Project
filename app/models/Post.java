package models;

import java.time.Instant;
import java.util.Objects;

/**
 * An immutable post independent of its REST or streaming source.
 *
 * <p>AI assistance: implementation support for the Phase 3 post model.</p>
 *
 * @author Nabeel Ali
 */
public final class Post {
    /** Stores the opaque post identifier. */
    private final String id;
    /** Stores the author display name. */
    private final String authorName;
    /** Stores the local or federated account handle. */
    private final String authorHandle;
    /** Stores the publication instant. */
    private final Instant createdAt;
    /** Stores the language code or Unknown. */
    private final String language;
    /** Stores the plain text post content. */
    private final String content;
    /** Stores the original HTTP or HTTPS post link. */
    private final String url;
    /** Stores the post visibility. */
    private final String visibility;

    /**
     * Creates a post from values validated by its data source.
     *
     * @param id the opaque post identifier
     * @param authorName the author display name
     * @param authorHandle the local or federated account handle
     * @param createdAt the publication instant
     * @param language the language code or Unknown
     * @param content the plain text post content
     * @param url the original HTTP or HTTPS post link
     * @param visibility the post visibility
     * @throws NullPointerException if any value is null
     * @author Nabeel Ali
     */
    public Post(String id, String authorName, String authorHandle, Instant createdAt,
            String language, String content, String url, String visibility) {
        // Final fields and immutable values keep cached posts safe to share.
        this.id = Objects.requireNonNull(id, "id");
        this.authorName = Objects.requireNonNull(authorName, "authorName");
        this.authorHandle = Objects.requireNonNull(authorHandle, "authorHandle");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.language = Objects.requireNonNull(language, "language");
        this.content = Objects.requireNonNull(content, "content");
        this.url = Objects.requireNonNull(url, "url");
        this.visibility = Objects.requireNonNull(visibility, "visibility");
    }

    /**
     * Returns the opaque post identifier.
     * @return the opaque post identifier
     * @author Nabeel Ali
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the author display name.
     * @return the author display name
     * @author Nabeel Ali
     */
    public String getAuthorName() {
        return authorName;
    }

    /**
     * Returns the local or federated account handle.
     * @return the local or federated account handle
     * @author Nabeel Ali
     */
    public String getAuthorHandle() {
        return authorHandle;
    }

    /**
     * Returns the publication instant.
     * @return the publication instant
     * @author Nabeel Ali
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the language code or Unknown.
     * @return the language code or Unknown
     * @author Nabeel Ali
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Returns the plain text post content.
     * @return the plain text post content
     * @author Nabeel Ali
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the original HTTP or HTTPS post link.
     * @return the original HTTP or HTTPS post link
     * @author Nabeel Ali
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the post visibility.
     * @return the post visibility
     * @author Nabeel Ali
     */
    public String getVisibility() {
        return visibility;
    }
}
