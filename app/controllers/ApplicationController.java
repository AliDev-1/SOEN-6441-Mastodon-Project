package controllers;

import jakarta.inject.Inject;
import services.SearchService;
import services.ReadabilityService;
import models.Post;
import models.SearchPage;
import services.MastodonException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * Handles browser requests and delegates application work to services.
 *
 * <p>AI assistance: implementation support for the Phase 2 request for one
 * asynchronous controller and a tested home page; Phase 6 search and error UI; Phase 7 pagination; Phase 8 cached readability and details.</p>
 *
 * @author Nabeel Ali
 */
public class ApplicationController extends Controller {

    /** Coordinates validated, cached searches. */
    private final SearchService searches;
    /** Calculates local readability estimates from the cached posts. */
    private final ReadabilityService readability;

    /**
     * Creates the application controller for Play dependency injection.
     *
     * @param searches the asynchronous search service
     * @param readability the local readability calculator
     * @author Nabeel Ali
     */
    @Inject
    public ApplicationController(SearchService searches, ReadabilityService readability) {
        this.searches = searches;
        this.readability = readability;
    }

    /**
     * Renders the home page without waiting for external work.
     *
     * @return a completed asynchronous response containing the home page
     * @author Nabeel Ali
     */
    public CompletionStage<Result> index() {
        // Keep the action asynchronous even though this page needs no API call.
        return CompletableFuture.completedFuture(ok(views.html.index.render()));
    }

    /**
     * Renders the first page of a validated hashtag search.
     * @param hashtag the submitted search term
     * @return an asynchronous results or error response
     * @author Nabeel Ali
     */
    public CompletionStage<Result> search(String hashtag) {
        // Service validation, caching and HTTP work remain outside the controller.
        return searches.search(hashtag, null)
                .thenApply(page -> renderPage(page, 1))
                .exceptionally(failure -> searchError(hashtag, failure));
    }

    /**
     * Displays an exact cached page without issuing a new API request.
     * @param id the snapshot identifier
     * @param pageNumber the position in this browsing sequence
     * @return the cached result page or an expiry error
     * @author Nabeel Ali
     */
    public CompletionStage<Result> cached(String id, int pageNumber) {
        if (invalidPageNumber(pageNumber)) {
            return CompletableFuture.completedFuture(searchError("", new MastodonException(400, "Invalid page number. Please search again.")));
        }
        return searches.cachedPage(id).thenApply(page -> renderPage(page, pageNumber))
                .exceptionally(failure -> searchError("", failure));
    }

    /**
     * Loads older results and redirects to their stable cached-page URL.
     * @param id the source snapshot identifier
     * @param pageNumber the current position in this browsing sequence
     * @return a redirect to the older snapshot or a safe error
     * @author Nabeel Ali
     */
    public CompletionStage<Result> older(String id, int pageNumber) {
        if (invalidPageNumber(pageNumber)) {
            return CompletableFuture.completedFuture(searchError("", new MastodonException(400, "Invalid page number. Please search again.")));
        }
        return searches.olderPage(id)
                .thenApply(page -> redirect(routes.ApplicationController.cached(page.getId(), pageNumber + 1)))
                .exceptionally(failure -> searchError("", failure));
    }

    /**
     * Renders a post and its readability from an existing snapshot without refetching.
     * @param id the snapshot identifier
     * @param postId the opaque post identifier within that snapshot
     * @param pageNumber the result page position for the return link
     * @return the post details or a controlled error response
     * @author Nabeel Ali
     */
    public CompletionStage<Result> details(String id, String postId, int pageNumber) {
        if (invalidPageNumber(pageNumber)) {
            return CompletableFuture.completedFuture(searchError("", new MastodonException(400, "Invalid page number. Please search again.")));
        }
        return searches.cachedPage(id).thenApply(page -> {
            Post post = readability.findPost(page, postId);
            return ok(views.html.details.render(page, post, readability.analyze(post), pageNumber));
        }).exceptionally(failure -> searchError("", failure));
    }

    /**
     * Renders results with a predecessor link only while that snapshot is cached.
     * @param page the completed result snapshot
     * @param pageNumber the position shown in the navigation
     * @return the successful HTML response
     * @author Nabeel Ali
     */
    private Result renderPage(SearchPage page, int pageNumber) {
        return ok(views.html.results.render(page, searches.previousPageId(page), pageNumber, readability.analyzePage(page)));
    }

    /**
     * Rejects invalid positions and prevents overflow when advancing a page.
     * @param pageNumber the requested display position
     * @return whether the page number cannot be used
     * @author Nabeel Ali
     */
    private boolean invalidPageNumber(int pageNumber) {
        return pageNumber < 1 || pageNumber == Integer.MAX_VALUE;
    }

    /**
     * Converts asynchronous failures into safe, useful browser responses.
     * @param hashtag the original input to preserve in the form
     * @param failure the failed operation
     * @return an HTML error response with its appropriate status
     * @author Nabeel Ali
     */
    private Result searchError(String hashtag, Throwable failure) {
        // Completion stages wrap failures; expose only our controlled messages.
        while (failure instanceof CompletionException && failure.getCause() != null) {
            failure = failure.getCause();
        }
        if (failure instanceof MastodonException error) {
            return status(error.getStatus(), views.html.searchError.render(hashtag, error.getMessage()));
        }
        return internalServerError(views.html.searchError.render(hashtag,
                "Something went wrong while searching. Please try again."));
    }
}
