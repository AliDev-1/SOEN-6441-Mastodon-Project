package services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import models.Post;

/**
 * Translates Mastodon timeline JSON into transport-independent posts.
 *
 * <p>AI assistance: implementation support for the Phase 3 request to validate
 * Mastodon responses and keep JSON outside the domain model.</p>
 *
 * @author Nabeel Ali
 */
public final class MastodonPostMapper {
    /** Parses a single complete JSON value without accepting trailing data. */
    private final ObjectMapper json;
    /** Converts content fragments into readable text. */
    private final HtmlText htmlText;

    /**
     * Creates reusable, locally configured parsing helpers.
     * @author Nabeel Ali
     */
    public MastodonPostMapper() {
        // Configure once before sharing this mapper between requests.
        json = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        htmlText = new HtmlText();
    }

    /**
     * Parses a timeline response, preserving API order and rejecting partial data.
     *
     * @param body the raw JSON response body
     * @return an unmodifiable list of posts, possibly empty
     * @throws IllegalArgumentException if the response or a required field is invalid
     * @author Nabeel Ali
     */
    public List<Post> parse(String body) {
        // Reject absent bodies before passing them to Jackson.
        if (body == null) {
            throw new IllegalArgumentException("Missing Mastodon response.");
        }
        try {
            JsonNode root = json.readTree(body);
            if (!root.isArray()) {
                throw new IllegalArgumentException("Expected a Mastodon post array.");
            }
            // Convert every entry before returning; never silently omit invalid posts.
            List<Post> posts = new ArrayList<>();
            for (JsonNode status : root) {
                posts.add(toPost(status));
            }
            return List.copyOf(posts);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid Mastodon JSON.", exception);
        }
    }

    /**
     * Maps the fields needed for display and analysis from one status.
     *
     * @param status one timeline entry
     * @return a validated post
     * @author Nabeel Ali
     */
    private Post toPost(JsonNode status) {
        // IDs remain strings: Mastodon does not guarantee numeric identifiers.
        String id = requiredText(status, "id");
        JsonNode account = status.path("account");
        String handle = requiredText(account, "acct");
        String author = optionalText(account, "display_name", handle);
        String language = optionalText(status, "language", "Unknown");
        String content = htmlText.toPlainText(text(status, "content"));
        String visibility = requiredText(status, "visibility");

        // A null HTML URL may fall back to the status's federation URI.
        String url = optionalText(status, "url", "");
        if (url.isEmpty()) {
            url = requiredText(status, "uri");
        }
        validateUrl(url);
        try {
            Instant createdAt = Instant.parse(requiredText(status, "created_at"));
            return new Post(id, author, handle, createdAt, language, content, url, visibility);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid Mastodon publication date.", exception);
        }
    }

    /**
     * Reads a required string while allowing empty content such as media-only posts.
     *
     * @param node the containing JSON value
     * @param field the field name
     * @return the field's exact string value
     * @author Nabeel Ali
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        // Do not coerce numbers, objects, booleans, or missing values into strings.
        if (!value.isTextual()) {
            throw new IllegalArgumentException("Expected text for Mastodon field: " + field);
        }
        return value.textValue();
    }

    /**
     * Reads a required nonblank string.
     *
     * @param node the containing JSON value
     * @param field the field name
     * @return the trimmed string
     * @author Nabeel Ali
     */
    private String requiredText(JsonNode node, String field) {
        String value = text(node, field).strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty Mastodon field: " + field);
        }
        return value;
    }

    /**
     * Reads an optional string, using a fallback for absent, null, or blank values.
     *
     * @param node the containing JSON value
     * @param field the field name
     * @param fallback the replacement for unavailable values
     * @return the trimmed string or fallback
     * @author Nabeel Ali
     */
    private String optionalText(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return fallback;
        }
        // Present values must still have the correct JSON type.
        String result = text(node, field).strip();
        return result.isEmpty() ? fallback : result;
    }

    /**
     * Ensures original-post links use an absolute HTTP or HTTPS address.
     *
     * @param url the proposed original-post link
     * @throws IllegalArgumentException if the link is malformed or unsupported
     * @author Nabeel Ali
     */
    private void validateUrl(String url) {
        URI uri = URI.create(url);
        // Reject executable schemes and relative URLs before they reach a template.
        boolean webScheme = "https".equalsIgnoreCase(uri.getScheme())
                || "http".equalsIgnoreCase(uri.getScheme());
        if (!webScheme || uri.getHost() == null) {
            throw new IllegalArgumentException("Expected an absolute HTTP(S) Mastodon post URL.");
        }
    }
}
