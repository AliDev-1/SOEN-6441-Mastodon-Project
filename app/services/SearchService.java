package services;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.typesafe.config.Config;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import models.SearchPage;

/**
 * Shares asynchronous page requests and retains bounded, expiring snapshots.
 * <p>AI assistance: implementation support for Phase 5 validation and caching; Phase 7 cached navigation.</p>
 * @author Nabeel Ali
 */
@Singleton
public final class SearchService {
    /** Retrieves pages without blocking. */
    private final MastodonClient client;
    /** Validates inputs before cache lookup. */
    private final SearchInput input;
    /** Separates results by configured server. */
    private final String instance;
    /** Holds at most 100 pages after cache maintenance. */
    private final AsyncCache<List<String>, SearchPage> pages;

    /**
     * Creates the application-wide cache with a monotonic clock.
     * @param client the asynchronous API adapter
     * @param config the application settings
     * @param input the input validator
     * @author Nabeel Ali
     */
    @Inject
    public SearchService(MastodonClient client, Config config, SearchInput input) {
        this(client, config, input, Ticker.systemTicker());
    }

    /**
     * Allows tests to advance time without sleeping.
     * @param client the API adapter
     * @param config the application settings
     * @param input the input validator
     * @param ticker the monotonic clock
     * @author Nabeel Ali
     */
    SearchService(MastodonClient client, Config config, SearchInput input, Ticker ticker) {
        this.client = client;
        this.input = input;
        this.instance = config.hasPath("mastodon.instance")
                ? config.getString("mastodon.instance").strip() : "";
        // Caffeine atomically shares pending loads and automatically removes failed loads.
        this.pages = Caffeine.newBuilder().maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(10)).ticker(ticker)
                .executor(Runnable::run).buildAsync();
    }

    /**
     * Returns a cached page or starts one shared asynchronous request.
     * @param hashtag the browser search term
     * @param cursor the optional pagination cursor
     * @return the page or a controlled failed stage
     * @author Nabeel Ali
     */
    public CompletionStage<SearchPage> search(String hashtag, String cursor) {
        try {
            String tag = input.hashtag(hashtag);
            String maxId = input.cursor(cursor);
            List<String> key = List.of(instance, tag, maxId);
            // Copy the future so caller cancellation cannot cancel the shared request.
            return pages.get(key, (ignored, executor) -> client.fetchPosts(tag, maxId)
                    .thenApply(posts -> new SearchPage(UUID.randomUUID().toString(), tag, maxId, posts))
                    .toCompletableFuture()).copy();
        } catch (MastodonException failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    /**
     * Retrieves the exact completed snapshot without fetching replacement data.
     * @param id the snapshot identifier from a result link
     * @return the page, or status 410 if expired, evicted or unknown
     * @author Nabeel Ali
     */
    public CompletionStage<SearchPage> cachedPage(String id) {
        // The synchronous view exposes only completed, unexpired values; no waits occur.
        for (SearchPage page : pages.synchronous().asMap().values()) {
            if (page.getId().equals(id)) {
                return CompletableFuture.completedFuture(page);
            }
        }
        return CompletableFuture.failedFuture(new MastodonException(410,
                "These search results have expired. Please search again."));
    }
    /**
     * Retrieves older posts using the exact cached source page's final post ID.
     * @param id the source snapshot identifier
     * @return a cached or newly retrieved older page
     * @author Nabeel Ali
     */
    public CompletionStage<SearchPage> olderPage(String id) {
        // Expired source pages fail before any replacement query can be issued.
        return cachedPage(id).thenCompose(page -> {
            String cursor = page.getNextCursor();
            if (cursor.isEmpty()) {
                return CompletableFuture.failedFuture(new MastodonException(400,
                        "There are no older posts to request from this page. Please search again."));
            }
            return search(page.getHashtag(), cursor);
        });
    }

    /**
     * Finds a still-cached predecessor using the cursor relationship, without fetching.
     * @param page the displayed snapshot
     * @return the predecessor snapshot ID, or an empty string if unavailable
     * @author Nabeel Ali
     */
    public String previousPageId(SearchPage page) {
        if (page.getCursor().isEmpty()) {
            return "";
        }
        // Both pages must belong to the same hashtag in this instance's cache.
        for (SearchPage candidate : pages.synchronous().asMap().values()) {
            if (candidate.getHashtag().equals(page.getHashtag())
                    && candidate.getNextCursor().equals(page.getCursor())) {
                return candidate.getId();
            }
        }
        return "";
    }
}
