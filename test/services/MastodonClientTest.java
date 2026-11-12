package services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import models.Post;
import org.junit.Before;
import org.junit.Test;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static play.inject.Bindings.bind;

/**
 * Verifies the client through mocked HTTP requests, never through the live API.
 * <p>AI assistance: implementation and test-design support for Phase 4 request
 * contracts, asynchronous completion, and failure partitions.</p>
 * @author Nabeel Ali
 */
public class MastodonClientTest {
    /** A synthetic status used only in local responses. */
    private static final String STATUS = """
            {"id":"one","account":{"acct":"alice"},"created_at":"2026-01-01T00:00:00Z",
             "content":"<p>Hello Java!</p>","url":"https://example.org/one","visibility":"public"}
            """;
    /** Mocked HTTP client. */
    private WSClient ws;
    /** Mocked fluent request builder. */
    private WSRequest request;
    /** Mocked upstream response. */
    private WSResponse response;
    /** Manually completed network operation. */
    private CompletableFuture<WSResponse> pending;
    /** Client under test. */
    private MastodonClient client;

    /**
     * Creates the JUnit fixture.
     * @author Nabeel Ali
     */
    public MastodonClientTest() {
        // Each test receives fresh mocks in setUp.
    }

    /**
     * Creates fresh mocks so no test can make a network request.
     * @author Nabeel Ali
     */
    @Before
    public void setUp() {
        ws = mock(WSClient.class);
        request = mock(WSRequest.class, RETURNS_SELF);
        response = mock(WSResponse.class);
        pending = new CompletableFuture<>();
        when(ws.url(anyString())).thenReturn(request);
        when(request.get()).thenReturn(pending);
        when(response.getStatus()).thenReturn(200);
        when(response.getBody()).thenReturn("[]");
        client = new MastodonClient(ws, configuration("https://example.org", "test-token"),
                new MastodonPostMapper());
    }

    /**
     * Checks endpoint, authorization, size, timeout, redirects, and pending behavior.
     * @author Nabeel Ali
     */
    @Test
    public void sendsBoundedNonBlockingRequest() {
        CompletionStage<List<Post>> result = client.fetchPosts("java", null);
        assertFalse(result.toCompletableFuture().isDone());
        verify(ws).url("https://example.org/api/v1/timelines/tag/java");
        verify(request).addHeader("Authorization", "Bearer test-token");
        verify(request).addHeader("Accept", "application/json");
        verify(request).addQueryParameter("limit", "40");
        verify(request).setRequestTimeout(Duration.ofSeconds(10));
        verify(request).setFollowRedirects(false);
        verify(request, never()).addQueryParameter(eq("max_id"), anyString());
        verify(request, times(1)).get();
        pending.complete(response);
        assertEquals(List.of(), result.toCompletableFuture().join());
    }

    /**
     * Encodes a path segment and supplies opaque pagination IDs as query values.
     * @author Nabeel Ali
     */
    @Test
    public void encodesTagAndAddsCursor() {
        client = new MastodonClient(ws, configuration(" https://example.org/ ", " test-token "),
                new MastodonPostMapper());
        client.fetchPosts("c++/café space", "opaque&id");
        verify(ws).url("https://example.org/api/v1/timelines/tag/c%2B%2B%2Fcaf%C3%A9%20space");
        verify(request).addQueryParameter("max_id", "opaque&id");
        verify(request).addHeader("Authorization", "Bearer test-token");
    }

    /**
     * Treats a blank cursor as the first page.
     * @author Nabeel Ali
     */
    @Test
    public void omitsBlankCursor() {
        client.fetchPosts("java", "  ");
        verify(request, never()).addQueryParameter(eq("max_id"), anyString());
    }

    /**
     * Returns a failed stage for absent hashtags without sending requests.
     * @author Nabeel Ali
     */
    @Test
    public void rejectsAbsentTags() {
        for (String tag : new String[] {null, "", " \t "}) {
            assertFailure(client.fetchPosts(tag, null), 400, "Enter a hashtag before searching.");
        }
        verifyNoInteractions(ws);
    }

    /**
     * Accepts pages at the one-post and forty-post boundaries and maps their text.
     * @author Nabeel Ali
     */
    @Test
    public void mapsSuccessfulPages() {
        pending.complete(response);
        for (int count : new int[] {1, 40}) {
            when(response.getBody()).thenReturn("[" + String.join(",", Collections.nCopies(count, STATUS)) + "]");
            List<Post> posts = client.fetchPosts("java", null).toCompletableFuture().join();
            assertEquals(count, posts.size());
            assertEquals("one", posts.get(0).getId());
            assertEquals("Hello Java!", posts.get(0).getContent());
            assertEquals("alice", posts.get(0).getAuthorHandle());
        }
    }

    /**
     * Rejects malformed, missing, and oversized successful response bodies.
     * @author Nabeel Ali
     */
    @Test
    public void rejectsInvalidResponseData() {
        pending.complete(response);
        for (String body : new String[] {null, "[", "{}", "[{}]",
                "[" + String.join(",", Collections.nCopies(41, STATUS)) + "]"}) {
            when(response.getBody()).thenReturn(body);
            assertFailure(client.fetchPosts("java", null), 502, "Mastodon returned invalid post data.");
        }
    }

    /**
     * Translates authentication, rate limiting, service, timeout, and other statuses.
     * @author Nabeel Ali
     */
    @Test
    public void mapsHttpFailures() {
        Object[][] cases = {
            {401, 502, "Mastodon authentication failed. Check the access token and read:statuses permission."},
            {403, 502, "Mastodon authentication failed. Check the access token and read:statuses permission."},
            {429, 503, "Mastodon is rate limiting requests. Please try again later."},
            {503, 503, "Mastodon is temporarily unavailable. Please try again later."},
            {504, 504, "Mastodon took too long to respond. Please try again later."},
            {500, 502, "Unable to retrieve posts from Mastodon. Please try again later."},
            {404, 502, "Unable to retrieve posts from Mastodon. Please try again later."},
            {302, 502, "Unable to retrieve posts from Mastodon. Please try again later."},
            {204, 502, "Unable to retrieve posts from Mastodon. Please try again later."}
        };
        pending.complete(response);
        for (Object[] entry : cases) {
            when(response.getStatus()).thenReturn((Integer) entry[0]);
            assertFailure(client.fetchPosts("java", null), (Integer) entry[1], (String) entry[2]);
        }
        // Error bodies may contain sensitive details and are never accessed.
        verify(response, never()).getBody();
    }

    /**
     * Recognizes timeouts even when asynchronous operations wrap their causes.
     * @author Nabeel Ali
     */
    @Test
    public void mapsTimeoutFailures() {
        for (Throwable failure : List.of(new TimeoutException("private details"),
                new CompletionException(new SocketTimeoutException("private details")))) {
            when(request.get()).thenReturn(CompletableFuture.failedFuture(failure));
            assertFailure(client.fetchPosts("java", null), 504,
                    "Mastodon took too long to respond. Please try again later.");
        }
    }

    /**
     * Reports failed connections without retaining the underlying exception message.
     * @author Nabeel Ali
     */
    @Test
    public void mapsConnectionFailures() {
        pending.completeExceptionally(new CompletionException(new ConnectException("private details")));
        assertFailure(client.fetchPosts("java", null), 503,
                "Unable to connect to Mastodon. Please try again later.");
    }

    /**
     * Handles synchronous request setup failures through the same failed-stage API.
     * @author Nabeel Ali
     */
    @Test
    public void mapsSynchronousFailures() {
        when(request.get()).thenThrow(new IllegalStateException("private details"));
        assertFailure(client.fetchPosts("java", null), 503,
                "Unable to connect to Mastodon. Please try again later.");
    }

    /**
     * Rejects each invalid origin component before attaching credentials.
     * @author Nabeel Ali
     */
    @Test
    public void rejectsInvalidOrigins() {
        for (String origin : List.of("", "http://example.org", "https:/no-host",
                "https://user@example.org", "https://example.org?q=1", "https://example.org#fragment",
                "https://example.org/api", "https://bad host")) {
            MastodonClient invalid = new MastodonClient(ws, configuration(origin, "test-token"),
                    new MastodonPostMapper());
            assertFailure(invalid.fetchPosts("java", null), 503,
                    "Configure a valid Mastodon HTTPS instance and access token.");
        }
        verifyNoInteractions(ws);
    }

    /**
     * Handles missing settings and empty credentials as controlled service errors.
     * @author Nabeel Ali
     */
    @Test
    public void rejectsMissingConfiguration() {
        for (Config config : List.of(ConfigFactory.empty(), configuration("https://example.org", " "),
                ConfigFactory.parseMap(Map.of("mastodon.accessToken", "test-token")),
                ConfigFactory.parseMap(Map.of("mastodon.instance", "https://example.org")))) {
            MastodonClient invalid = new MastodonClient(ws, config, new MastodonPostMapper());
            assertFailure(invalid.fetchPosts("java", null), 503,
                    "Configure a valid Mastodon HTTPS instance and access token.");
        }
        verifyNoInteractions(ws);
    }

    /**
     * Checks that Play can inject the client with a mocked network boundary.
     * @author Nabeel Ali
     */
    @Test
    public void supportsDependencyInjection() {
        // Start and stop an isolated Play app with its HTTP client replaced by a mock.
        var app = new GuiceApplicationBuilder()
                .configure(configuration("https://example.org", "test-token"))
                .overrides(bind(WSClient.class).toInstance(ws)).build();
        play.test.Helpers.running(app, () -> {
            MastodonClient injected = app.injector().instanceOf(MastodonClient.class);
            pending.complete(response);
            assertEquals(List.of(), injected.fetchPosts("java", null).toCompletableFuture().join());
            assertSame(injected, app.injector().instanceOf(MastodonClient.class));
        });
    }

    /**
     * Builds configuration containing only synthetic test credentials.
     * @param instance the test origin
     * @param token the dummy token
     * @return isolated configuration
     * @author Nabeel Ali
     */
    private Config configuration(String instance, String token) {
        return ConfigFactory.parseMap(Map.of("mastodon.instance", instance, "mastodon.accessToken", token));
    }

    /**
     * Checks the precise safe status, message, and absence of raw exception causes.
     * @param stage the failed request
     * @param status the expected application status
     * @param message the expected user-facing message
     * @author Nabeel Ali
     */
    private void assertFailure(CompletionStage<List<Post>> stage, int status, String message) {
        // Blocking waits are confined to tests and finish from completed mock futures.
        CompletionException error = assertThrows(CompletionException.class, () -> stage.toCompletableFuture().join());
        assertTrue(error.getCause() instanceof MastodonException);
        MastodonException failure = (MastodonException) error.getCause();
        assertEquals(status, failure.getStatus());
        assertEquals(message, failure.getMessage());
        assertNull(failure.getCause());
    }
}
