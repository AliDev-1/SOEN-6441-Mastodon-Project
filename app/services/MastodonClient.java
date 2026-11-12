package services;

import com.typesafe.config.Config;
import java.net.URI;
import java.net.URLEncoder;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Singleton;
import models.Post;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

/**
 * Retrieves one bounded Mastodon timeline page using non-blocking Play WS calls.
 *
 * <p>AI assistance: implementation support for Phase 4 asynchronous requests,
 * external configuration, and controlled failures.</p>
 * @author Nabeel Ali
 */
@Singleton
public final class MastodonClient {
    /** Play owns this client's lifecycle. */
    private final WSClient ws;
    /** Maps response bodies to domain values. */
    private final MastodonPostMapper mapper;
    /** The externally configured instance origin. */
    private final String instance;
    /** The externally configured user token; never logged. */
    private final String token;

    /**
     * Injects the HTTP client, external settings, and post mapper.
     * @param ws the Play-managed HTTP client
     * @param config the application configuration
     * @param mapper the post response mapper
     * @author Nabeel Ali
     */
    @Inject
    public MastodonClient(WSClient ws, Config config, MastodonPostMapper mapper) {
        this.ws = ws;
        this.mapper = mapper;
        // Missing credentials must not prevent the home page from starting.
        instance = setting(config, "mastodon.instance");
        token = setting(config, "mastodon.accessToken");
    }

    /**
     * Fetches at most 40 posts without waiting for the network response.
     * The search service will apply full hashtag validation and caching.
     *
     * @param hashtag the hashtag without its leading hash
     * @param maxId an optional pagination cursor; null or blank means the first page
     * @return an asynchronous post list or a failed stage containing a safe error
     * @author Nabeel Ali
     */
    public CompletionStage<List<Post>> fetchPosts(String hashtag, String maxId) {
        try {
            if (hashtag == null || hashtag.isBlank()) {
                throw new MastodonException(400, "Enter a hashtag before searching.");
            }
            // Encode the path segment; user input cannot alter the API endpoint.
            String tag = URLEncoder.encode(hashtag, StandardCharsets.UTF_8).replace("+", "%20");
            WSRequest request = ws.url(origin() + "/api/v1/timelines/tag/" + tag)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Accept", "application/json")
                    .addQueryParameter("limit", "40")
                    .setRequestTimeout(Duration.ofSeconds(10))
                    .setFollowRedirects(false);
            if (maxId != null && !maxId.isBlank()) {
                request = request.addQueryParameter("max_id", maxId);
            }
            // Redirects are disabled so credentials stay on the configured origin.
            return request.get().thenApply(this::readResponse).exceptionally(failure -> {
                throw safeFailure(failure);
            });
        } catch (RuntimeException failure) {
            // Even a synchronous setup failure follows the asynchronous contract.
            return CompletableFuture.failedFuture(safeFailure(failure));
        }
    }

    /**
     * Reads an optional configuration string without printing its value.
     * @param config the application configuration
     * @param key the setting name
     * @return a trimmed setting or an empty string
     * @author Nabeel Ali
     */
    private String setting(Config config, String key) {
        return config.hasPath(key) ? config.getString(key).strip() : "";
    }

    /**
     * Validates the configured HTTPS origin before attaching authorization.
     * @return the instance origin without a trailing slash
     * @author Nabeel Ali
     */
    private String origin() {
        try {
            URI uri = URI.create(instance);
            if (token.isEmpty() || !"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !List.of("", "/").contains(uri.getPath())) {
                throw new IllegalArgumentException();
            }
            return instance.endsWith("/") ? instance.substring(0, instance.length() - 1) : instance;
        } catch (IllegalArgumentException failure) {
            // Do not retain configuration values in the exception or its cause.
            throw new MastodonException(503, "Configure a valid Mastodon HTTPS instance and access token.");
        }
    }

    /**
     * Maps successful content and translates upstream status codes.
     * @param response the completed HTTP response
     * @return the parsed posts
     * @author Nabeel Ali
     */
    private List<Post> readResponse(WSResponse response) {
        // A timeline success is HTTP 200; other responses must not reach the mapper.
        switch (response.getStatus()) {
            case 200:
                try {
                    List<Post> posts = mapper.parse(response.getBody());
                    if (posts.size() > 40) {
                        throw new IllegalArgumentException();
                    }
                    return posts;
                } catch (IllegalArgumentException failure) {
                    throw new MastodonException(502, "Mastodon returned invalid post data.");
                }
            case 401:
            case 403:
                throw new MastodonException(502, "Mastodon authentication failed. Check the access token and read:statuses permission.");
            case 429:
                throw new MastodonException(503, "Mastodon is rate limiting requests. Please try again later.");
            case 503:
                throw new MastodonException(503, "Mastodon is temporarily unavailable. Please try again later.");
            case 504:
                throw new MastodonException(504, "Mastodon took too long to respond. Please try again later.");
            default:
                throw new MastodonException(502, "Unable to retrieve posts from Mastodon. Please try again later.");
        }
    }

    /**
     * Unwraps asynchronous failures and discards potentially sensitive raw details.
     * @param failure the failed operation
     * @return a controlled application error
     * @author Nabeel Ali
     */
    private MastodonException safeFailure(Throwable failure) {
        // CompletionStage can wrap the original failure in CompletionException.
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof MastodonException known) {
                return known;
            }
            if (cause instanceof TimeoutException || cause instanceof SocketTimeoutException) {
                return new MastodonException(504, "Mastodon took too long to respond. Please try again later.");
            }
        }
        return new MastodonException(503, "Unable to connect to Mastodon. Please try again later.");
    }
}
