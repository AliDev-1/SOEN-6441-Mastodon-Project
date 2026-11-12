package models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Checks immutable snapshots and required values.
 * <p>AI assistance: test-design support for Phase 5 page values.</p>
 * @author Nabeel Ali
 */
public class SearchPageTest {
    /** Creates the test fixture.
     * @author Nabeel Ali
     */
    public SearchPageTest() { }

    /** Verifies defensive copies, getters, and null rejection.
     * @author Nabeel Ali
     */
    @Test
    public void preservesImmutableValues() {
        Post post = new Post("1", "Author", "author", Instant.EPOCH, "en", "Text", "https://example.org/1", "public");
        List<Post> posts = new ArrayList<>(List.of(post));
        SearchPage page = new SearchPage("snapshot", "java", "cursor", posts);
        posts.clear();
        assertEquals("snapshot", page.getId());
        assertEquals("java", page.getHashtag());
        assertEquals("cursor", page.getCursor());
        assertEquals(List.of(post), page.getPosts());
        assertThrows(UnsupportedOperationException.class, () -> page.getPosts().clear());
        assertThrows(NullPointerException.class, () -> new SearchPage(null, "java", "", List.of()));
        assertThrows(NullPointerException.class, () -> new SearchPage("id", null, "", List.of()));
        assertThrows(NullPointerException.class, () -> new SearchPage("id", "java", null, List.of()));
        assertThrows(NullPointerException.class, () -> new SearchPage("id", "java", "", null));
    }
}
