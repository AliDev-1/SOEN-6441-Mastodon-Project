package controllers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;

/**
 * Verifies the home action, its route, and the stylesheet used by the page.
 *
 * <p>AI assistance: implementation and test-design support for the Phase 2
 * request to verify concrete responses without calling the live Mastodon API,
 * updated in Phase 6 for the search form.</p>
 *
 * @author Nabeel Ali
 */
public class ApplicationControllerTest extends WithApplication {

    /**
     * Creates the test fixture managed by JUnit and Play.
     *
     * @author Nabeel Ali
     */
    public ApplicationControllerTest() {
        // WithApplication starts and stops an isolated Play app for each test.
    }

    /**
     * Checks the asynchronous action directly, including its returned content.
     *
     * @author Nabeel Ali
     */
    @Test
    public void indexReturnsHomePage() {
        // Waiting for a future is permitted in tests, but not production code.
        Result result = new ApplicationController(org.mockito.Mockito.mock(services.SearchService.class), new services.ReadabilityService()).index().toCompletableFuture().join();
        assertHomePage(result);
    }

    /**
     * Checks that the public home route reaches the application controller.
     *
     * @author Nabeel Ali
     */
    @Test
    public void homeRouteReturnsHomePage() {
        // Exercise routing, dependency injection, and rendering together.
        Result result = route(app, new Http.RequestBuilder().method("GET").uri("/"));
        assertHomePage(result);
    }

    /**
     * Checks that the stylesheet linked by the rendered page can be retrieved.
     *
     * @author Nabeel Ali
     */
    @Test
    public void homeStylesheetIsServed() {
        // Read the actual asset URL so fingerprinted assets work as well.
        Result home = route(app, new Http.RequestBuilder().method("GET").uri("/"));
        Document document = Jsoup.parse(contentAsString(home));
        String stylesheet = document.selectFirst("link[rel=stylesheet]").attr("href");
        Result result = route(app, new Http.RequestBuilder().method("GET").uri(stylesheet));
        assertEquals(200, result.status());
        assertEquals("text/css", result.contentType().orElse(""));
        // Play serves files through its HTTP runtime; use its materializer to read them.
        String css = contentAsString(result,
                app.injector().instanceOf(org.apache.pekko.stream.Materializer.class));
        assertTrue(css.contains("font-family: system-ui, sans-serif;"));
    }

    /**
     * Checks the expected response status, document metadata, and visible text.
     *
     * @param result the response returned by the action or route
     * @author Nabeel Ali
     */
    private void assertHomePage(Result result) {
        // Assert the specified content rather than merely checking for a body.
        assertNotNull(result);
        assertEquals(200, result.status());
        assertEquals("text/html", result.contentType().orElse(""));
        Document document = Jsoup.parse(contentAsString(result));
        assertEquals("en", document.selectFirst("html").attr("lang"));
        assertEquals("MAWA | Mastodon Analysis Web App", document.title());
        assertEquals("Mastodon Analysis Web App", document.selectFirst("h1").text());
        assertEquals("/search", document.selectFirst("form").attr("action"));
        assertEquals("get", document.selectFirst("form").attr("method"));
        assertEquals("hashtag", document.selectFirst("input").attr("name"));
        assertEquals("Search posts", document.selectFirst("button").text());
    }
}
