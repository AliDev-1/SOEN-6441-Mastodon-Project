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
 * <p>AI assistance: test-design support for the Phase 6 search interface.</p>
 * @author Nabeel Ali
 */
public class SearchControllerTest extends WithApplication {
    /** Mocked service for deterministic result pages. */
    private final SearchService searches = mock(SearchService.class);

    /** Creates the isolated test fixture.
     * @author Nabeel Ali
     */
    public SearchControllerTest() { }

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
        Result result = new ApplicationController(searches).search(input).toCompletableFuture().join();
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
        var result = new ApplicationController(searches).search("java").toCompletableFuture();
        assertFalse(result.isDone());
        pending.completeExceptionally(new IllegalStateException("private credential details"));
        assertEquals(500, result.join().status());
        assertFalse(contentAsString(result.join()).contains("private credential details"));
        assertTrue(contentAsString(result.join()).contains("Something went wrong while searching. Please try again."));
        // A wrapper without a cause is also handled without revealing its text.
        when(searches.search("java", null)).thenReturn(CompletableFuture.failedFuture(new CompletionException("private", null)));
        assertEquals(500, new ApplicationController(searches).search("java").toCompletableFuture().join().status());
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

}
