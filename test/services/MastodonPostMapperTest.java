package services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.List;
import models.Post;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Verifies timeline mapping with synthetic JSON and no network calls.
 *
 * <p>AI assistance: test-design and implementation support for the Phase 3
 * request for exact mapping assertions and malformed-data partitions.</p>
 * @author Nabeel Ali
 */
public class MastodonPostMapperTest {
    /** The mapper under test. */
    private final MastodonPostMapper mapper = new MastodonPostMapper();
    /** Builds and edits synthetic response fixtures. */
    private final ObjectMapper json = new ObjectMapper();

    /**
     * Creates an isolated mapper fixture.
     * @author Nabeel Ali
     */
    public MastodonPostMapperTest() {
        // All response bodies are local fixtures, never live API responses.
    }

    /**
     * Checks every mapped field, API order, and protection against list mutation.
     * @throws JsonProcessingException if the fixed test fixture is invalid
     * @author Nabeel Ali
     */
    @Test
    public void mapsValuesInApiOrder() throws JsonProcessingException {
        ObjectNode first = status();
        ObjectNode second = status().put("id", "older-id");
        first.put("extra_field", "ignored");
        List<Post> posts = mapper.parse(json.createArrayNode().add(first).add(second).toString());
        assertEquals(2, posts.size());
        Post post = posts.get(0);
        assertEquals("opaque-id", post.getId());
        assertEquals("Alice", post.getAuthorName());
        assertEquals("alice@example.org", post.getAuthorHandle());
        assertEquals(Instant.parse("2026-01-01T12:00:00Z"), post.getCreatedAt());
        assertEquals("en", post.getLanguage());
        assertEquals("Hello & welcome! Next line.", post.getContent());
        assertEquals("https://example.org/@alice/1", post.getUrl());
        assertEquals("public", post.getVisibility());
        assertEquals("older-id", posts.get(1).getId());
        assertThrows(UnsupportedOperationException.class, () -> posts.add(post));
    }

    /**
     * Accepts a successful empty timeline.
     * @author Nabeel Ali
     */
    @Test
    public void acceptsEmptyArray() {
        assertEquals(List.of(), mapper.parse("[]"));
    }

    /**
     * Rejects absent, malformed, trailing, and non-array JSON bodies.
     * @author Nabeel Ali
     */
    @Test
    public void rejectsInvalidBodies() {
        assertEquals("Missing Mastodon response.",
                assertThrows(IllegalArgumentException.class, () -> mapper.parse(null)).getMessage());
        for (String input : List.of("[", "not json", "[] {}")) {
            assertEquals("Invalid Mastodon JSON.",
                    assertThrows(IllegalArgumentException.class, () -> mapper.parse(input)).getMessage());
        }
        for (String input : List.of("", "  ", "null", "{}", "42", "true", "\"text\"")) {
            assertEquals("Expected a Mastodon post array.",
                    assertThrows(IllegalArgumentException.class, () -> mapper.parse(input)).getMessage());
        }
    }

    /**
     * Rejects non-object array entries rather than returning partial results.
     * @throws JsonProcessingException if the fixed test fixture is invalid
     * @author Nabeel Ali
     */
    @Test
    public void rejectsInvalidEntries() throws JsonProcessingException {
        for (String invalid : List.of("null", "1", "true", "[]", "\"text\"", "{}")) {
            String body = "[" + status() + "," + invalid + "]";
            assertEquals("Expected text for Mastodon field: id",
                    assertThrows(IllegalArgumentException.class, () -> mapper.parse(body)).getMessage());
        }
    }

    /**
     * Checks missing, null, wrong-type, and blank required text fields separately.
     * @throws JsonProcessingException if the fixed test fixture is invalid
     * @author Nabeel Ali
     */
    @Test
    public void rejectsInvalidRequiredFields() throws JsonProcessingException {
        for (String field : List.of("id", "created_at", "visibility", "content")) {
            ObjectNode input = status();
            input.remove(field);
            assertRejected(input, "Expected text for Mastodon field: " + field);
            input.putNull(field);
            assertRejected(input, "Expected text for Mastodon field: " + field);
            input.put(field, 123);
            assertRejected(input, "Expected text for Mastodon field: " + field);
        }
        for (String field : List.of("id", "created_at", "visibility")) {
            assertRejected(status().put(field, " \t "), "Empty Mastodon field: " + field);
        }
    }

    /**
     * Checks author object and handle validation.
     * @throws JsonProcessingException if the fixed test fixture is invalid
     * @author Nabeel Ali
     */
    @Test
    public void rejectsInvalidAccounts() throws JsonProcessingException {
        ObjectNode input = status();
        input.remove("account");
        assertRejected(input, "Expected text for Mastodon field: acct");
        for (String invalid : List.of("null", "[]", "1", "{}", "{\"acct\":null}", "{\"acct\":false}")) {
            input.set("account", json.readTree(invalid));
            assertRejected(input, "Expected text for Mastodon field: acct");
        }
        input.set("account", json.createObjectNode().put("acct", " "));
        assertRejected(input, "Empty Mastodon field: acct");
    }

    /**
     * Uses explicit defaults for missing, null, and blank optional strings.
     * @throws JsonProcessingException if the fixed test fixture is invalid
     * @author Nabeel Ali
     */
    @Test
    public void defaultsOptionalFields() throws JsonProcessingException {
        for (String replacement : List.of("missing", "null", "\" \"")) {
            ObjectNode input = status();
            ObjectNode account = (ObjectNode) input.get("account");
            for (String field : List.of("language", "url", "display_name")) {
                ObjectNode owner = field.equals("display_name") ? account : input;
                if (replacement.equals("missing")) {
                    owner.remove(field);
                } else {
                    owner.set(field, json.readTree(replacement));
                }
            }
            Post post = mapOne(input);
            assertEquals("Unknown", post.getLanguage());
            assertEquals("alice@example.org", post.getAuthorName());
            assertEquals("https://example.org/users/alice/statuses/1", post.getUrl());
        }
    }

    /**
     * Rejects optional fields that are present with an incorrect type.
     * @throws JsonProcessingException if the fixed test fixture is invalid
     * @author Nabeel Ali
     */
    @Test
    public void rejectsIncorrectOptionalTypes() throws JsonProcessingException {
        for (String field : List.of("language", "url", "display_name")) {
            ObjectNode input = status();
            ObjectNode owner = field.equals("display_name") ? (ObjectNode) input.get("account") : input;
            owner.put(field, false);
            assertRejected(input, "Expected text for Mastodon field: " + field);
        }
    }

    /**
     * Accepts media-only posts and normalizes surrounding metadata whitespace.
     * @throws JsonProcessingException if the fixed test fixture is invalid
     * @author Nabeel Ali
     */
    @Test
    public void acceptsEmptyContentAndTrimsMetadata() throws JsonProcessingException {
        ObjectNode input = status().put("id", " opaque-id ").put("language", " fr ")
                .put("content", "").put("created_at", "2026-01-01T13:00:00+01:00");
        Post post = mapOne(input);
        assertEquals("", post.getContent());
        assertEquals("opaque-id", post.getId());
        assertEquals("fr", post.getLanguage());
        assertEquals(Instant.parse("2026-01-01T12:00:00Z"), post.getCreatedAt());
    }

    /**
     * Rejects malformed and impossible publication timestamps.
     * @throws JsonProcessingException if the fixed test fixture is invalid
     * @author Nabeel Ali
     */
    @Test
    public void rejectsInvalidDates() throws JsonProcessingException {
        for (String date : List.of("yesterday", "2026-02-30T12:00:00Z", "2026-01-01")) {
            assertRejected(status().put("created_at", date), "Invalid Mastodon publication date.");
        }
    }

    /**
     * Accepts both web schemes and rejects executable, relative, and hostless links.
     * @throws JsonProcessingException if the fixed test fixture is invalid
     * @author Nabeel Ali
     */
    @Test
    public void validatesOriginalLinks() throws JsonProcessingException {
        for (String url : List.of("http://example.org/1", "HTTPS://example.org/1")) {
            assertEquals(url, mapOne(status().put("url", url)).getUrl());
        }
        for (String url : List.of("javascript:alert(1)", "ftp://example.org/1", "/relative", "https:/missing-host")) {
            assertRejected(status().put("url", url), "Expected an absolute HTTP(S) Mastodon post URL.");
        }
        ObjectNode malformed = status().put("url", "https://example.org/bad path");
        assertThrows(IllegalArgumentException.class, () -> mapOne(malformed));
        ObjectNode noLink = status();
        noLink.remove(List.of("url", "uri"));
        assertRejected(noLink, "Expected text for Mastodon field: uri");
        noLink.put("uri", " ");
        assertRejected(noLink, "Empty Mastodon field: uri");
    }

    /**
     * Builds a representative synthetic status for each independent test case.
     * @return a mutable copy of the valid fixture
     * @throws JsonProcessingException if the fixed fixture is invalid
     * @author Nabeel Ali
     */
    private ObjectNode status() throws JsonProcessingException {
        return (ObjectNode) json.readTree("""
                {
                  "id": "opaque-id",
                  "created_at": "2026-01-01T12:00:00Z",
                  "account": {"acct": "alice@example.org", "display_name": "Alice"},
                  "language": "en",
                  "content": "<p>Hello &amp; welcome!</p><p>Next<br>line.</p>",
                  "url": "https://example.org/@alice/1",
                  "uri": "https://example.org/users/alice/statuses/1",
                  "visibility": "public"
                }
                """);
    }

    /**
     * Wraps one status in a timeline array and returns its mapped post.
     * @param status the synthetic status
     * @return the single post
     * @author Nabeel Ali
     */
    private Post mapOne(ObjectNode status) {
        ArrayNode timeline = json.createArrayNode().add(status);
        return mapper.parse(timeline.toString()).get(0);
    }

    /**
     * Verifies a malformed status produces the specified validation message.
     * @param status the malformed status
     * @param message the expected error message
     * @author Nabeel Ali
     */
    private void assertRejected(ObjectNode status, String message) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> mapOne(status));
        assertEquals(message, error.getMessage());
    }
}
