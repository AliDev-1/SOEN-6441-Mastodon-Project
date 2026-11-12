package services;

import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import models.Post;
import models.SearchPage;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Checks validation, shared loads, snapshot identity and bounded retention.
 * <p>AI assistance: test-design support for Phase 5 cache behavior and Phase 7 pagination.</p>
 * @author Nabeel Ali
 */
public class SearchServiceTest {
    /** Mocked API boundary. */
    private final MastodonClient client = mock(MastodonClient.class);
    /** Controllable monotonic time. */
    private final AtomicLong clock = new AtomicLong();
    /** Service under test. */
    private final SearchService service = new SearchService(client,
            ConfigFactory.parseString("mastodon.instance=\"https://example.org\""),
            new SearchInput(), clock::get);

    /** Verifies a Phase 5 behavior with synthetic data.
     * @author Nabeel Ali
     */
    public SearchServiceTest() { }

    /** Verifies a Phase 5 behavior with synthetic data.
     * @author Nabeel Ali
     */
    @Test
    public void validatesAndNormalizesInput() {
        SearchInput input = new SearchInput();
        assertEquals("café_2", input.hashtag(" CAFE\u0301_2 "));
        assertEquals("日本語", input.hashtag("日本語"));
        assertEquals("a".repeat(100), input.hashtag("a".repeat(100)));
        for (String value : new String[]{null, "", " ", "#java", "two words", "a/b", "123_", "😀", "a".repeat(101)}) {
            assertEquals(400, assertThrows(MastodonException.class, () -> input.hashtag(value)).getStatus());
        }
        assertEquals("", input.cursor(null));
        assertEquals("", input.cursor("  "));
        assertEquals("A:b+1", input.cursor(" A:b+1 "));
        assertEquals("x".repeat(256), input.cursor("x".repeat(256)));
        for (String value : new String[]{"a b", "a\nb", "a\u0000b", "a\u00a0b", "x".repeat(257)}) {
            assertEquals(400, assertThrows(MastodonException.class, () -> input.cursor(value)).getStatus());
        }
        assertFailure(service.search("#java", null), 400);
        assertFailure(service.search("java", "a b"), 400);
        verifyNoInteractions(client);
    }

    /** Verifies a Phase 5 behavior with synthetic data.
     * @author Nabeel Ali
     */
    @Test
    public void sharesPendingLoadsAndProtectsAgainstCancellation() {
        CompletableFuture<List<Post>> pending = new CompletableFuture<>();
        when(client.fetchPosts("java", "")).thenReturn(pending);
        CompletableFuture<SearchPage> first = service.search(" Java ", null).toCompletableFuture();
        CompletableFuture<SearchPage> second = service.search("java", "").toCompletableFuture();
        assertFalse(second.isDone());
        assertFailure(service.cachedPage("pending"), 410);
        first.cancel(true);
        assertFalse(pending.isCancelled());
        pending.complete(List.of());
        SearchPage page = second.join();
        assertEquals("java", page.getHashtag());
        assertEquals("", page.getCursor());
        assertTrue(page.getPosts().isEmpty());
        assertSame(page, service.search("JAVA", null).toCompletableFuture().join());
        assertSame(page, service.cachedPage(page.getId()).toCompletableFuture().join());
        assertFailure(service.cachedPage(null), 410);
        verify(client, times(1)).fetchPosts("java", "");
    }

    /** Verifies a Phase 5 behavior with synthetic data.
     * @author Nabeel Ali
     */
    @Test
    public void separatesQueriesAndRetriesFailedLoads() {
        CompletableFuture<List<Post>> failed = new CompletableFuture<>();
        when(client.fetchPosts("java", "")).thenReturn(failed, CompletableFuture.completedFuture(List.of()));
        CompletionStage<SearchPage> result = service.search("java", null);
        failed.completeExceptionally(new MastodonException(503, "Unavailable"));
        assertFailure(result, 503);
        SearchPage page = service.search("java", null).toCompletableFuture().join();
        when(client.fetchPosts("java", "cursor")).thenReturn(CompletableFuture.completedFuture(List.of()));
        when(client.fetchPosts("testing", "")).thenReturn(CompletableFuture.completedFuture(List.of()));
        SearchPage next = service.search("java", "cursor").toCompletableFuture().join();
        assertEquals("cursor", next.getCursor());
        assertNotEquals(page.getId(), next.getId());
        assertNotEquals(page.getId(), service.search("testing", null).toCompletableFuture().join().getId());
        verify(client, times(2)).fetchPosts("java", "");
    }

    /** Verifies a Phase 5 behavior with synthetic data.
     * @author Nabeel Ali
     */
    @Test
    public void expiresSnapshotsWithoutRefreshingAnalysis() {
        when(client.fetchPosts("java", "")).thenReturn(CompletableFuture.completedFuture(List.of()));
        SearchPage original = service.search("java", null).toCompletableFuture().join();
        clock.set(Duration.ofMinutes(10).toNanos() - 1);
        assertSame(original, service.cachedPage(original.getId()).toCompletableFuture().join());
        clock.incrementAndGet();
        assertFailure(service.cachedPage(original.getId()), 410);
        verify(client, times(1)).fetchPosts("java", "");
        SearchPage refreshed = service.search("java", null).toCompletableFuture().join();
        assertNotEquals(original.getId(), refreshed.getId());
        assertFailure(service.cachedPage(original.getId()), 410);
        verify(client, times(2)).fetchPosts("java", "");
    }

    /** Verifies a Phase 5 behavior with synthetic data.
     * @author Nabeel Ali
     */
    @Test
    public void boundsRetainedPages() {
        when(client.fetchPosts(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(List.of()));
        List<SearchPage> results = new ArrayList<>();
        for (int index = 0; index < 120; index++) {
            results.add(service.search("tag" + index, null).toCompletableFuture().join());
        }
        long retained = results.stream().filter(page -> !service.cachedPage(page.getId())
                .toCompletableFuture().isCompletedExceptionally()).count();
        assertEquals(100, retained);
        verify(client, times(120)).fetchPosts(anyString(), anyString());
    }

    /** Verifies a Phase 5 behavior with synthetic data.
     * @author Nabeel Ali
     */
    @Test
    public void supportsDefaultConstructionAndMissingSettings() {
        when(client.fetchPosts("java", "")).thenReturn(CompletableFuture.completedFuture(List.of()));
        SearchService defaultService = new SearchService(client, ConfigFactory.empty(), new SearchInput());
        assertEquals("java", defaultService.search("java", null).toCompletableFuture().join().getHashtag());
    }

    /** Verifies simultaneous callers share one pending API operation.
     * @throws Exception if a worker cannot finish within the test timeout
     * @author Nabeel Ali
     */
    @Test
    public void sharesConcurrentRequests() throws Exception {
        CompletableFuture<List<Post>> pending = new CompletableFuture<>();
        when(client.fetchPosts("java", "")).thenReturn(pending);
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var ready = new java.util.concurrent.CountDownLatch(8);
            var start = new java.util.concurrent.CountDownLatch(1);
            var calls = new ArrayList<java.util.concurrent.Future<CompletionStage<SearchPage>>>();
            // Release eight callers together while the API response remains pending.
            for (int index = 0; index < 8; index++) {
                calls.add(workers.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, java.util.concurrent.TimeUnit.SECONDS));
                    return service.search("java", null);
                }));
            }
            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            var stages = new ArrayList<CompletionStage<SearchPage>>();
            for (var call : calls) {
                stages.add(call.get(5, java.util.concurrent.TimeUnit.SECONDS));
            }
            verify(client, times(1)).fetchPosts("java", "");
            pending.complete(List.of());
            SearchPage page = stages.getFirst().toCompletableFuture().join();
            for (var stage : stages) {
                assertSame(page, stage.toCompletableFuture().join());
            }
        }
    }

    /** Verifies failures already completed by the client are not retained.
     * @author Nabeel Ali
     */
    @Test
    public void retriesImmediatelyFailedLoads() {
        when(client.fetchPosts("java", "")).thenReturn(
                CompletableFuture.failedFuture(new MastodonException(503, "Unavailable")),
                CompletableFuture.completedFuture(List.of()));
        assertFailure(service.search("java", null), 503);
        assertTrue(service.search("java", null).toCompletableFuture().join().getPosts().isEmpty());
        verify(client, times(2)).fetchPosts("java", "");
    }

    /** Checks cursor selection, shared older requests and cached predecessor navigation.
     * @author Nabeel Ali
     */
    @Test
    public void navigatesCachedPages() {
        when(client.fetchPosts("java", "")).thenReturn(CompletableFuture.completedFuture(List.of(post("newest"), post("opaque:id+1"))));
        when(client.fetchPosts("java", "opaque:id+1")).thenReturn(CompletableFuture.completedFuture(List.of(post("last"))));
        when(client.fetchPosts("java", "last")).thenReturn(CompletableFuture.completedFuture(List.of()));
        SearchPage first = service.search("java", null).toCompletableFuture().join();
        SearchPage second = service.olderPage(first.getId()).toCompletableFuture().join();
        assertEquals("opaque:id+1", second.getCursor());
        assertSame(second, service.olderPage(first.getId()).toCompletableFuture().join());
        assertEquals("", service.previousPageId(first));
        assertEquals(first.getId(), service.previousPageId(second));
        SearchPage end = service.olderPage(second.getId()).toCompletableFuture().join();
        assertTrue(end.getPosts().isEmpty());
        assertEquals(second.getId(), service.previousPageId(end));
        assertFailure(service.olderPage(end.getId()), 400);
        assertFailure(service.olderPage("missing"), 410);
        verify(client, times(1)).fetchPosts("java", "");
        verify(client, times(1)).fetchPosts("java", "opaque:id+1");
        verify(client, times(1)).fetchPosts("java", "last");
        verifyNoMoreInteractions(client);
    }

    /** Checks unavailable and unrelated predecessor snapshots, and source expiry.
     * @author Nabeel Ali
     */
    @Test
    public void doesNotRefetchExpiredNavigation() {
        when(client.fetchPosts("other", "")).thenReturn(CompletableFuture.completedFuture(List.of(post("shared"))));
        when(client.fetchPosts("java", "")).thenReturn(CompletableFuture.completedFuture(List.of(post("shared"))));
        service.search("other", null).toCompletableFuture().join();
        assertEquals("", service.previousPageId(new SearchPage("detached", "java", "shared", List.of())));
        SearchPage first = service.search("java", null).toCompletableFuture().join();
        assertEquals("", service.previousPageId(new SearchPage("unmatched", "java", "absent", List.of())));
        clock.set(Duration.ofMinutes(10).toNanos());
        assertEquals("", service.previousPageId(new SearchPage("expired-parent", "java", "shared", List.of())));
        assertFailure(service.olderPage(first.getId()), 410);
        verify(client, times(1)).fetchPosts("java", "");
        verify(client, times(1)).fetchPosts("other", "");
        verifyNoMoreInteractions(client);
    }

    /** Creates a synthetic post with an opaque identifier.
     * @param id the post identifier
     * @return a test post
     * @author Nabeel Ali
     */
    private static Post post(String id) {
        return new Post(id, "Author", "author", java.time.Instant.EPOCH, "en", "Text", "https://example.org/post", "public");
    }

    /** Checks a controlled asynchronous error.
     * @param stage the failed operation
     * @param status the expected status
     * @author Nabeel Ali
     */
    private static void assertFailure(CompletionStage<?> stage, int status) {
        CompletionException failure = assertThrows(CompletionException.class, () -> stage.toCompletableFuture().join());
        assertEquals(status, ((MastodonException) failure.getCause()).getStatus());
    }
}
