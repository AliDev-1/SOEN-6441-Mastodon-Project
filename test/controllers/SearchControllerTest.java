package controllers;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import models.Post;
import models.SearchPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import services.MastodonException;
import services.SearchService;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static play.inject.Bindings.bind;
import static play.test.Helpers.*;

/**
 * Exercises search routing, rendering and asynchronous errors without live HTTP.
 * <p>AI assistance: test-design support for the Phase 6 search interface, Phase 7 pagination, and Phase 8 readability.</p>
 * @author Nabeel Ali
 */
public class SearchControllerTest extends WithApplication {
    /** Mocked service for deterministic result pages. */
    private final SearchService searches = mock(SearchService.class);

    /** Creates the isolated test fixture.
     * @author Nabeel Ali
     */
    public SearchControllerTest() {
        when(searches.previousPageId(any(SearchPage.class))).thenReturn("");
    }

    /** Replaces the service before Play constructs the controller.
     * @return the isolated application
     * @author Nabeel Ali
     */
    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder().overrides(bind(SearchService.class).toInstance(searches)).build();
    }

    /** Checks metadata, result order and escaping through the public search route.
     * @author Nabeel Ali
     */
    @Test
    public void rendersPostsAndEscapesUntrustedText() {
        Post first = new Post("1", "<script>alert(1)</script>", "author@example.org", Instant.EPOCH,
                "en", "<img src=x onerror=alert(1)> & text", "https://example.org/posts/1?a=1&b=2", "public");
        Post second = new Post("2", "Second", "second", Instant.EPOCH, "Unknown", "", "https://example.org/2", "unlisted");
        when(searches.search("Java", null)).thenReturn(CompletableFuture.completedFuture(
                new SearchPage("snapshot", "java", "", List.of(first, second))));
        Result result = route(app, new Http.RequestBuilder().method("GET").uri("/search?hashtag=Java"));
        assertEquals(200, result.status());
        assertEquals("text/html", result.contentType().orElse(""));
        Document doc = Jsoup.parse(contentAsString(result));
        assertEquals("Results for #java", doc.selectFirst("h1").text());
        assertEquals("java", doc.selectFirst("input").val());
        assertEquals("snapshot", doc.selectFirst("section").attr("data-snapshot"));
        assertEquals(2, doc.select("article").size());
        assertEquals(first.getAuthorName(), doc.selectFirst(".author").text());
        assertEquals("@author@example.org", doc.selectFirst(".handle").text());
        assertEquals("1970-01-01T00:00:00Z", doc.selectFirst("time").attr("datetime"));
        assertEquals(List.of("1970-01-01T00:00:00Z", "en", "public"), doc.selectFirst("article").select("dd").eachText());
        assertEquals(first.getContent(), doc.selectFirst(".post-content").text());
        assertEquals(first.getUrl(), doc.selectFirst(".original-link").attr("href"));
        assertEquals("Second", doc.select(".author").get(1).text());
        assertEquals("This post has no text content.", doc.select(".post-content").get(1).text());
        assertTrue(doc.select("script, img").isEmpty());
        verify(searches).search("Java", null);
    }

    /** Checks the empty success state rather than displaying an error.
     * @author Nabeel Ali
     */
    @Test
    public void rendersEmptySearch() {
        when(searches.search("nothing", null)).thenReturn(CompletableFuture.completedFuture(
                new SearchPage("empty", "nothing", "", List.of())));
        Result result = route(app, new Http.RequestBuilder().method("GET").uri("/search?hashtag=nothing"));
        assertEquals(200, result.status());
        Document doc = Jsoup.parse(contentAsString(result));
        assertEquals("No posts found for this hashtag. Try another hashtag.", doc.getElementById("no-results").text());
        assertEquals("0 posts on this page (up to 40).", doc.selectFirst(".result-count").text());
        assertTrue(doc.select("article").isEmpty());
    }

    /** Checks default route parameters and controlled error codes and messages.
     * @author Nabeel Ali
     */
    @Test
    public void rendersControlledErrors() {
        for (int status : new int[]{400, 502, 503, 504, 410}) {
            when(searches.search("", null)).thenReturn(CompletableFuture.failedFuture(
                    new MastodonException(status, "Controlled explanation")));
            Result result = route(app, new Http.RequestBuilder().method("GET").uri("/search"));
            assertEquals(status, result.status());
            Document doc = Jsoup.parse(contentAsString(result));
            assertEquals("Controlled explanation", doc.selectFirst("[role=alert]").text());
            assertEquals("", doc.selectFirst("input").val());
        }
    }

    /** Checks that error forms preserve input as text, with no injected elements.
     * @author Nabeel Ali
     */
    @Test
    public void escapesInvalidInputOnError() {
        String input = "\"><script>bad()</script>";
        when(searches.search(input, null)).thenReturn(CompletableFuture.failedFuture(new MastodonException(400, "Invalid hashtag")));
        Result result = new ApplicationController(searches, new services.ReadabilityService()).search(input).toCompletableFuture().join();
        Document doc = Jsoup.parse(contentAsString(result));
        assertEquals(input, doc.selectFirst("input").val());
        assertTrue(doc.select("script").isEmpty());
    }

    /** Checks nonblocking completion and safe fallback for unknown failures.
     * @author Nabeel Ali
     */
    @Test
    public void waitsAsynchronouslyAndHidesUnexpectedFailures() {
        CompletableFuture<SearchPage> pending = new CompletableFuture<>();
        when(searches.search("java", null)).thenReturn(pending);
        var result = new ApplicationController(searches, new services.ReadabilityService()).search("java").toCompletableFuture();
        assertFalse(result.isDone());
        pending.completeExceptionally(new IllegalStateException("private credential details"));
        assertEquals(500, result.join().status());
        assertFalse(contentAsString(result.join()).contains("private credential details"));
        assertTrue(contentAsString(result.join()).contains("Something went wrong while searching. Please try again."));
        // A wrapper without a cause is also handled without revealing its text.
        when(searches.search("java", null)).thenReturn(CompletableFuture.failedFuture(new CompletionException("private", null)));
        assertEquals(500, new ApplicationController(searches, new services.ReadabilityService()).search("java").toCompletableFuture().join().status());
    }
    /** Verifies real validation and cache reuse across repeated browser requests.
     * @author Nabeel Ali
     */
    @Test
    public void integratesValidationAndCaching() {
        services.MastodonClient client = mock(services.MastodonClient.class);
        when(client.fetchPosts("java", "")).thenReturn(CompletableFuture.completedFuture(List.of()));
        Application integrated = new GuiceApplicationBuilder()
                .overrides(bind(services.MastodonClient.class).toInstance(client)).build();
        running(integrated, () -> {
            // Invalid browser input never reaches the mocked API boundary.
            Result invalid = route(integrated, new Http.RequestBuilder().method("GET").uri("/search?hashtag=%23java"));
            assertEquals(400, invalid.status());
            verifyNoInteractions(client);
            Result first = route(integrated, new Http.RequestBuilder().method("GET").uri("/search?hashtag=Java"));
            Result repeated = route(integrated, new Http.RequestBuilder().method("GET").uri("/search?hashtag=java"));
            assertEquals(200, first.status());
            assertEquals(contentAsString(first), contentAsString(repeated));
            verify(client, times(1)).fetchPosts("java", "");
        });
    }

    /** Follows rendered pagination links through the real service and a mocked API.
     * @author Nabeel Ali
     */
    @Test
    public void followsOlderAndPreviousLinksWithoutDuplicateRequests() {
        services.MastodonClient client = mock(services.MastodonClient.class);
        Post firstPost = new Post("opaque:last", "Author", "author", Instant.EPOCH, "en", "Text", "https://example.org/1", "public");
        when(client.fetchPosts("java", "")).thenReturn(CompletableFuture.completedFuture(List.of(firstPost)));
        when(client.fetchPosts("java", "opaque:last")).thenReturn(CompletableFuture.completedFuture(List.of()));
        Application integrated = new GuiceApplicationBuilder()
                .overrides(bind(services.MastodonClient.class).toInstance(client)).build();
        running(integrated, () -> {
            Result first = route(integrated, new Http.RequestBuilder().method("GET").uri("/search?hashtag=java"));
            Document initial = Jsoup.parse(contentAsString(first));
            assertTrue(initial.select(".previous-page, .first-page").isEmpty());
            assertEquals(List.of("Page 1", "Page 1"), initial.select(".page-number").eachText());
            assertEquals(2, initial.select("nav.page-navigation").size());
            String nextUrl = initial.selectFirst(".older-page").attr("href");
            Result next = route(integrated, new Http.RequestBuilder().method("GET").uri(nextUrl));
            assertEquals(303, next.status());
            String cachedUrl = next.redirectLocation().orElseThrow();
            Result empty = route(integrated, new Http.RequestBuilder().method("GET").uri(cachedUrl));
            assertEquals(200, empty.status());
            Document end = Jsoup.parse(contentAsString(empty));
            assertTrue(end.select(".older-page").isEmpty());
            assertEquals(List.of("Page 2", "Page 2"), end.select(".page-number").eachText());
            assertEquals("← Previous page", end.selectFirst(".previous-page").text());
            assertEquals("Open saved version of this page", end.selectFirst(".cached-page").text());
            assertEquals(cachedUrl, end.selectFirst(".cached-page").attr("href"));
            assertTrue(end.selectFirst(".cache-note").text().contains("same posts"));
            assertEquals("No older posts were returned. You have reached the end of the available results.", end.getElementById("no-results").text());
            assertEquals("/search?hashtag=java", end.selectFirst(".first-page").attr("href"));
            String previousUrl = end.selectFirst(".previous-page").attr("href");
            assertEquals(initial.selectFirst(".cached-page").attr("href"), previousUrl);
            Result previous = route(integrated, new Http.RequestBuilder().method("GET").uri(previousUrl));
            assertEquals(contentAsString(first), contentAsString(previous));
            Result again = route(integrated, new Http.RequestBuilder().method("GET").uri(nextUrl));
            assertEquals(cachedUrl, again.redirectLocation().orElseThrow());
            verify(client, times(1)).fetchPosts("java", "");
            verify(client, times(1)).fetchPosts("java", "opaque:last");
            verifyNoMoreInteractions(client);
        });
    }

    /** Checks both navigation failure paths retain safe status codes and messages.
     * @author Nabeel Ali
     */
    @Test
    public void rendersNavigationFailures() {
        when(searches.cachedPage("expired")).thenReturn(CompletableFuture.failedFuture(new MastodonException(410, "Please search again.")));
        when(searches.olderPage("expired")).thenReturn(CompletableFuture.failedFuture(new MastodonException(410, "Please search again.")));
        for (String url : List.of("/results/expired", "/results/expired/older")) {
            Result result = route(app, new Http.RequestBuilder().method("GET").uri(url));
            assertEquals(410, result.status());
            assertEquals("Please search again.", Jsoup.parse(contentAsString(result)).selectFirst("[role=alert]").text());
        }
    }
    /** Checks numbered URLs preserve position while navigating shared snapshots.
     * @author Nabeel Ali
     */
    @Test
    public void preservesPageNumbersInLinks() {
        Post post = new Post("last", "Author", "author", Instant.EPOCH, "en", "Text", "https://example.org/1", "public");
        SearchPage page = new SearchPage("third", "java", "cursor", List.of(post));
        when(searches.cachedPage("third")).thenReturn(CompletableFuture.completedFuture(page));
        when(searches.previousPageId(page)).thenReturn("second");
        when(searches.olderPage("third")).thenReturn(CompletableFuture.completedFuture(new SearchPage("fourth", "java", "last", List.of())));
        Result result = route(app, new Http.RequestBuilder().method("GET").uri("/results/third?pageNumber=3"));
        Document doc = Jsoup.parse(contentAsString(result));
        assertEquals(List.of("Page 3", "Page 3"), doc.select(".page-number").eachText());
        assertEquals("/results/second?pageNumber=2", doc.selectFirst(".previous-page").attr("href"));
        assertEquals("/results/third?pageNumber=3", doc.selectFirst(".cached-page").attr("href"));
        assertEquals("Next page · Older posts →", doc.selectFirst(".older-page").text());
        Result next = route(app, new Http.RequestBuilder().method("GET").uri(doc.selectFirst(".older-page").attr("href")));
        assertEquals("/results/fourth?pageNumber=4", next.redirectLocation().orElseThrow());
        // A missing predecessor disables backward navigation while keeping the position.
        when(searches.previousPageId(page)).thenReturn("");
        Result detached = route(app, new Http.RequestBuilder().method("GET").uri("/results/third?pageNumber=3"));
        assertTrue(Jsoup.parse(contentAsString(detached)).select("a.previous-page").isEmpty());
    }

    /** Rejects invalid display numbers without touching cached or external data.
     * @author Nabeel Ali
     */
    @Test
    public void rejectsInvalidPageNumbers() {
        for (String number : List.of("0", "-1", "2147483647", "not-a-number")) {
            for (String path : List.of("/results/id", "/results/id/older")) {
                Result result = route(app, new Http.RequestBuilder().method("GET").uri(path + "?pageNumber=" + number));
                assertEquals(400, result.status());
            }
        }
        verifyNoInteractions(searches);
    }
    /** Follows a result's details link and confirms identical scores without refetching.
     * @author Nabeel Ali
     */
    @Test
    public void showsReadabilityFromExactCachedPost() {
        services.MastodonClient client = mock(services.MastodonClient.class);
        Post post = new Post("opaque:post", "<script>Author</script>", "author", Instant.EPOCH,
                "en", "The cat sat on the mat.", "https://example.org/1", "public");
        when(client.fetchPosts("java", "")).thenReturn(CompletableFuture.completedFuture(List.of(post)));
        Application integrated = new GuiceApplicationBuilder()
                .overrides(bind(services.MastodonClient.class).toInstance(client)).build();
        running(integrated, () -> {
            Result search = route(integrated, new Http.RequestBuilder().method("GET").uri("/search?hashtag=java"));
            Document overview = Jsoup.parse(contentAsString(search));
            assertEquals("116.15", overview.selectFirst(".reading-ease").text());
            assertEquals("-1.45", overview.selectFirst(".grade-level").text());
            String detailsUrl = overview.selectFirst(".details-link").attr("href");
            assertEquals(detailsUrl, overview.selectFirst(".author a").attr("href"));
            Result details = route(integrated, new Http.RequestBuilder().method("GET").uri(detailsUrl));
            assertEquals(200, details.status());
            Document doc = Jsoup.parse(contentAsString(details));
            assertEquals("Post details", doc.selectFirst("h1").text());
            assertEquals(post.getAuthorName(), doc.selectFirst(".author").text());
            assertTrue(doc.select("script").isEmpty());
            assertEquals(post.getContent(), doc.selectFirst(".post-content").text());
            assertEquals(overview.selectFirst(".reading-ease").text(), doc.selectFirst(".reading-ease").text());
            assertEquals(overview.selectFirst(".grade-level").text(), doc.selectFirst(".grade-level").text());
            assertEquals("6", doc.selectFirst(".word-count").text());
            assertEquals("1", doc.selectFirst(".sentence-count").text());
            assertEquals("6", doc.selectFirst(".syllable-count").text());
            assertEquals(post.getUrl(), doc.selectFirst(".original-link").attr("href"));
            Result back = route(integrated, new Http.RequestBuilder().method("GET").uri(doc.selectFirst(".back-to-results").attr("href")));
            assertEquals(contentAsString(search), contentAsString(back));
            verify(client, times(1)).fetchPosts("java", "");
            verifyNoMoreInteractions(client);
        });
    }

    /** Checks unavailable scores, escaped detail content, and unknown-language labels.
     * @author Nabeel Ali
     */
    @Test
    public void rendersReadabilityAvailability() {
        Post empty = new Post("empty", "Author", "author", Instant.EPOCH, "en", "", "https://example.org/1", "public");
        Post french = new Post("fr", "Author", "author", Instant.EPOCH, "fr", "<img src=x onerror=bad()> Bonjour", "https://example.org/2", "public");
        Post unknown = new Post("unknown", "Author", "author", Instant.EPOCH, "Unknown", "Cat", "https://example.org/3", "public");
        when(searches.cachedPage("snapshot")).thenReturn(CompletableFuture.completedFuture(new SearchPage("snapshot", "java", "cursor", List.of(empty, french, unknown))));
        for (Post post : List.of(empty, french)) {
            Result result = route(app, new Http.RequestBuilder().method("GET").uri("/results/snapshot/posts/" + post.getId() + "?pageNumber=3"));
            assertEquals(200, result.status());
            Document doc = Jsoup.parse(contentAsString(result));
            assertTrue(doc.select(".reading-ease, .grade-level, .readability-counts, img").isEmpty());
            assertTrue(doc.selectFirst(".readability-note").text().contains("unavailable"));
            assertEquals("/results/snapshot?pageNumber=3", doc.selectFirst(".back-to-results").attr("href"));
        }
        Result result = route(app, new Http.RequestBuilder().method("GET").uri("/results/snapshot/posts/unknown"));
        assertTrue(Jsoup.parse(contentAsString(result)).selectFirst(".readability-note").text().contains("language is unknown"));
    }

    /** Checks invalid navigation, expired snapshots and post IDs absent from a snapshot.
     * @author Nabeel Ali
     */
    @Test
    public void handlesDetailErrors() {
        Result invalid = route(app, new Http.RequestBuilder().method("GET").uri("/results/snapshot/posts/post?pageNumber=0"));
        assertEquals(400, invalid.status());
        verifyNoInteractions(searches);
        when(searches.cachedPage("expired")).thenReturn(CompletableFuture.failedFuture(new MastodonException(410, "Please search again.")));
        Result expired = route(app, new Http.RequestBuilder().method("GET").uri("/results/expired/posts/post"));
        assertEquals(410, expired.status());
        when(searches.cachedPage("snapshot")).thenReturn(CompletableFuture.completedFuture(new SearchPage("snapshot", "java", "", List.of())));
        Result missing = route(app, new Http.RequestBuilder().method("GET").uri("/results/snapshot/posts/missing"));
        assertEquals(404, missing.status());
        assertTrue(contentAsString(missing).contains("not part of the saved results"));
    }

    /** Checks the details action returns before a pending snapshot lookup completes.
     * @author Nabeel Ali
     */
    @Test
    public void resolvesDetailsAsynchronously() {
        CompletableFuture<SearchPage> pending = new CompletableFuture<>();
        when(searches.cachedPage("pending")).thenReturn(pending);
        var result = new ApplicationController(searches, new services.ReadabilityService()).details("pending", "missing", 1).toCompletableFuture();
        assertFalse(result.isDone());
        pending.complete(new SearchPage("pending", "java", "", List.of()));
        assertEquals(404, result.join().status());
    }
}
