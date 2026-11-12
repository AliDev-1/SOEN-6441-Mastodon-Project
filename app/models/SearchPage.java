package models;

import java.util.List;
import java.util.Objects;

/**
 * An immutable snapshot shared by result display and later analyses.
 * <p>AI assistance: implementation support for Phase 5 cached page values.</p>
 * @author Nabeel Ali
 */
public final class SearchPage {
    /** Identifies this particular response, even after the same query is refreshed. */
    private final String id;
    /** Stores the normalized search term. */
    private final String hashtag;
    /** Stores the requested cursor; empty means the first page. */
    private final String cursor;
    /** Stores the posts in API order. */
    private final List<Post> posts;

    /**
     * Creates a snapshot with a defensive copy of the posts.
     * @param id the unique snapshot identifier
     * @param hashtag the normalized term
     * @param cursor the requested cursor
     * @param posts the retrieved posts
     * @author Nabeel Ali
     */
    public SearchPage(String id, String hashtag, String cursor, List<Post> posts) {
        // Immutable values prevent callers from changing a shared result.
        this.id = Objects.requireNonNull(id, "id");
        this.hashtag = Objects.requireNonNull(hashtag, "hashtag");
        this.cursor = Objects.requireNonNull(cursor, "cursor");
        this.posts = List.copyOf(posts);
    }

    /** Returns the snapshot identifier.
     * @return the snapshot identifier
     * @author Nabeel Ali
     */
    public String getId() { return id; }

    /** Returns the normalized hashtag.
     * @return the normalized hashtag
     * @author Nabeel Ali
     */
    public String getHashtag() { return hashtag; }

    /** Returns the requested cursor.
     * @return the requested cursor
     * @author Nabeel Ali
     */
    public String getCursor() { return cursor; }

    /** Returns the cached posts.
     * @return the unmodifiable posts in API order
     * @author Nabeel Ali
     */
    public List<Post> getPosts() { return posts; }
}
