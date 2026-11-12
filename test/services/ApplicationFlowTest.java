package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import controllers.ApplicationController;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static play.inject.Bindings.bind;
import static play.test.Helpers.*;

/**
 * Exercises real application layers together while mocking only outbound HTTP.
 * <p>AI assistance: test-design support for Phase 10 full-flow verification.</p>
 * @author Nabeel Ali
 */
public class ApplicationFlowTest extends WithApplication {
    /** Outbound transport boundary; no live requests are made. */
    private final WSClient ws = mock(WSClient.class);
    /** Records request configuration and supplies synthetic completions. */
    private final WSRequest request = mock(WSRequest.class, RETURNS_SELF);
    /** Dummy external settings used only in isolated applications. */
    private final Config config = ConfigFactory.parseString(
            "mastodon.instance=\"https://example.org\"\nmastodon.accessToken=\"test-token\"");

    /** Creates the transport fixture.
     * @author Nabeel Ali
     */
    public ApplicationFlowTest() {
        when(ws.url(anyString())).thenReturn(request);
    }

    /** Builds the real dependency graph with an isolated mock transport.
     * @return the test application
     * @author Nabeel Ali
     */
    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder().configure(config)
                .overrides(bind(WSClient.class).toInstance(ws)).build();
    }

    /** Follows a full page through both analyses, older results, and cached return links.
     * @author Nabeel Ali
     */
    @Test
    public void followsCompleteFlowFromJsonToAnalyses() {
        WSResponse firstResponse = response(200, timeline(40, "<p>Cat dog.</p>"));
        WSResponse secondResponse = response(200, timeline(1, "<p>Bird.</p>", 60));
        when(request.get()).thenReturn(CompletableFuture.completedFuture(firstResponse),
                CompletableFuture.completedFuture(secondResponse));

        // Every layer from JSON parsing to HTML rendering is real in this test.
        Result firstResult = get(app, "/search?hashtag=Java");
        Document first = document(firstResult);
        assertEquals(40, first.select("article.post").size());
        assertEquals("Cat dog.", first.selectFirst(".post-content").text());
        assertEquals("en", first.selectFirst("article").select("dd").get(1).text());
        assertEquals("120.21", first.selectFirst(".reading-ease").text());
        String firstUrl = first.selectFirst(".cached-page").attr("href");

        Document details = document(get(app, first.selectFirst(".details-link").attr("href")));
        assertEquals("2", details.selectFirst(".word-count").text());
        assertEquals(firstUrl, details.selectFirst(".back-to-results").attr("href"));
        Document statistics = document(get(app, first.selectFirst(".word-statistics-link").attr("href")));
        assertEquals("80", statistics.selectFirst(".total-words").text());
        assertEquals(List.of("cat 40", "dog 40"), statistics.select("tbody tr").eachText());

        // Only navigating to an uncached cursor issues the second HTTP request.
        Result redirect = get(app, first.selectFirst(".older-page").attr("href"));
        assertEquals(303, redirect.status());
        Document second = document(get(app, redirect.redirectLocation().orElseThrow()));
        assertEquals("Page 2", second.selectFirst(".page-number").text());
        assertEquals("Bird.", second.selectFirst(".post-content").text());
        Document secondStatistics = document(get(app, second.selectFirst(".word-statistics-link").attr("href")));
        assertEquals("1", secondStatistics.selectFirst(".total-words").text());
        assertEquals(List.of("bird 1"), secondStatistics.select("tbody tr").eachText());
        assertEquals(contentAsString(firstResult), contentAsString(get(app, second.selectFirst(".previous-page").attr("href"))));
        get(app, first.selectFirst(".older-page").attr("href"));
        verify(request, times(2)).get();
        verify(request, times(2)).addQueryParameter("limit", "40");
        verify(request).addQueryParameter("max_id", "61");
        verify(ws, times(2)).url("https://example.org/api/v1/timelines/tag/java");
    }

    /** Checks actual transport error translation reaches the browser and failures are retryable.
     * @author Nabeel Ali
     */
    @Test
    public void translatesErrorsAcrossAllLayersAndRetries() {
        int[][] statuses = {{401, 502}, {403, 502}, {429, 503}, {503, 503}, {504, 504}, {500, 502}};
        for (int[] pair : statuses) {
            respondWith(pair[0], "private upstream body");
            Result result = get(app, "/search?hashtag=errors");
            assertEquals(pair[1], result.status());
            assertFalse(contentAsString(result).contains("private upstream body"));
            assertFalse(contentAsString(result).contains("test-token"));
            assertNotNull(Jsoup.parse(contentAsString(result)).selectFirst("[role=alert]"));
        }
        // Malformed JSON and oversized responses fail before partial results can render.
        for (String body : List.of("{broken", timeline(41, "<p>Cat.</p>"))) {
            respondWith(200, body);
            Result result = get(app, "/search?hashtag=errors");
            assertEquals(502, result.status());
            assertTrue(contentAsString(result).contains("invalid post data"));
        }
        respondWith(200, "[]");
        assertEquals(200, get(app, "/search?hashtag=errors").status());
        assertEquals(200, get(app, "/search?hashtag=errors").status());
        verify(request, times(9)).get();
    }

    /** Checks pending transport requests remain asynchronous and concurrent searches share them.
     * @author Nabeel Ali
     */
    @Test
    public void sharesPendingTransportRequests() {
        CompletableFuture<WSResponse> pending = new CompletableFuture<>();
        when(request.get()).thenReturn(pending);
        ApplicationController controller = app.injector().instanceOf(ApplicationController.class);
        var first = controller.search("Java").toCompletableFuture();
        var second = controller.search("java").toCompletableFuture();
        assertFalse(first.isDone());
        assertFalse(second.isDone());
        verify(request, times(1)).get();
        pending.complete(response(200, timeline(1, "<p>Cat.</p>")));
        assertEquals(200, first.join().status());
        assertEquals(contentAsString(first.join()), contentAsString(second.join()));
    }

    /** Checks expired links across all analysis/navigation actions without a hidden refetch.
     * @author Nabeel Ali
     */
    @Test
    public void expiresFullFlowSnapshotsWithoutRefetching() {
        AtomicLong clock = new AtomicLong();
        SearchService searches = new SearchService(new MastodonClient(ws, config, new MastodonPostMapper()),
                config, new SearchInput(), clock::get);
        Application timed = new GuiceApplicationBuilder().configure(config)
                .overrides(bind(WSClient.class).toInstance(ws), bind(SearchService.class).toInstance(searches)).build();
        respondWith(200, timeline(1, "<p>Cat.</p>"));
        running(timed, () -> {
            Document first = document(get(timed, "/search?hashtag=java"));
            clock.set(Duration.ofMinutes(10).toNanos());
            for (String selector : List.of(".cached-page", ".older-page", ".details-link", ".word-statistics-link")) {
                Result expired = get(timed, first.selectFirst(selector).attr("href"));
                assertEquals(410, expired.status());
                assertTrue(contentAsString(expired).contains("Please search again"));
            }
            verify(request, times(1)).get();
            Document fresh = document(get(timed, "/search?hashtag=java"));
            assertNotEquals(first.selectFirst(".cached-page").attr("href"), fresh.selectFirst(".cached-page").attr("href"));
            verify(request, times(2)).get();
        });
    }

    /** Performs a browser-style GET through Play routing.
     * @param application the isolated application
     * @param url the rendered or explicit local URL
     * @return the route response
     * @author Nabeel Ali
     */
    private Result get(Application application, String url) {
        return route(application, new Http.RequestBuilder().method("GET").uri(url));
    }

    /** Asserts success before parsing an HTML page.
     * @param result the response
     * @return the parsed document
     * @author Nabeel Ali
     */
    private Document document(Result result) {
        assertEquals(200, result.status());
        return Jsoup.parse(contentAsString(result));
    }

    /** Creates a mocked HTTP response without a network connection.
     * @param status the HTTP status
     * @param body the synthetic response body
     * @return the response mock
     * @author Nabeel Ali
     */
    private WSResponse response(int status, String body) {
        WSResponse response = mock(WSResponse.class);
        when(response.getStatus()).thenReturn(status);
        when(response.getBody()).thenReturn(body);
        return response;
    }

    /** Prepares an immediate response without nesting Mockito stubbing calls.
     * @param status the HTTP status
     * @param body the synthetic body
     * @author Nabeel Ali
     */
    private void respondWith(int status, String body) {
        WSResponse prepared = response(status, body);
        when(request.get()).thenReturn(CompletableFuture.completedFuture(prepared));
    }

    /** Builds bounded or deliberately oversized JSON timelines with descending IDs.
     * @param count the number of synthetic posts
     * @param content the HTML content
     * @return the JSON array string
     * @author Nabeel Ali
     */
    private String timeline(int count, String content) {
        return timeline(count, content, 100);
    }

    /** Builds a synthetic timeline starting at a specified opaque ID value.
     * @param count the number of posts
     * @param content the HTML content
     * @param newestId the first synthetic numeric ID
     * @return the JSON array string
     * @author Nabeel Ali
     */
    private String timeline(int count, String content, int newestId) {
        var posts = new ObjectMapper().createArrayNode();
        for (int index = 0; index < count; index++) {
            var post = posts.addObject();
            post.put("id", Integer.toString(newestId - index));
            post.putObject("account").put("acct", "author").put("display_name", "Author");
            post.put("created_at", "2026-01-01T00:00:00Z");
            post.put("language", "en");
            post.put("content", content);
            post.put("url", "https://example.org/posts/" + (newestId - index));
            post.put("visibility", "public");
        }
        return posts.toString();
    }
}
