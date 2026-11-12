package models;

import java.time.Instant;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Checks the post value contract independently of Play and Mastodon JSON.
 *
 * <p>AI assistance: test-design support for Phase 3 immutable domain values.</p>
 * @author Nabeel Ali
 */
public class PostTest {
    /**
     * Creates the JUnit fixture.
     * @author Nabeel Ali
     */
    public PostTest() {
        // Tests create their own independent post values.
    }

    /**
     * Checks every stored value, including empty content and an opaque identifier.
     * @author Nabeel Ali
     */
    @Test
    public void preservesAllValues() {
        // Model values need no REST client or JSON object.
        Instant published = Instant.parse("2026-01-01T12:00:00Z");
        Post post = new Post("opaque-id", "Alice", "alice@example.org", published,
                "Unknown", "", "https://example.org/@alice/1", "public");
        assertEquals("opaque-id", post.getId());
        assertEquals("Alice", post.getAuthorName());
        assertEquals("alice@example.org", post.getAuthorHandle());
        assertEquals(published, post.getCreatedAt());
        assertEquals("Unknown", post.getLanguage());
        assertEquals("", post.getContent());
        assertEquals("https://example.org/@alice/1", post.getUrl());
        assertEquals("public", post.getVisibility());
    }

    /**
     * Rejects each null constructor argument with the responsible field name.
     * @author Nabeel Ali
     */
    @Test
    public void rejectsNullValues() {
        Object[] values = {"1", "Alice", "alice", Instant.EPOCH,
                "en", "Hello", "https://example.org/1", "public"};
        String[] names = {"id", "authorName", "authorHandle", "createdAt",
                "language", "content", "url", "visibility"};
        // Change just one argument at a time so another failure cannot mask it.
        for (int i = 0; i < values.length; i++) {
            Object[] input = values.clone();
            input[i] = null;
            NullPointerException error = assertThrows(NullPointerException.class,
                    () -> new Post((String) input[0], (String) input[1], (String) input[2],
                            (Instant) input[3], (String) input[4], (String) input[5],
                            (String) input[6], (String) input[7]));
            assertEquals(names[i], error.getMessage());
        }
    }
}
