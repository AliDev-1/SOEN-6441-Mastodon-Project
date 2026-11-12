package controllers;

import jakarta.inject.Inject;
import services.SearchService;
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
 * asynchronous controller and a tested home page; Phase 6 search and error UI.</p>
 *
 * @author Nabeel Ali
 */
public class ApplicationController extends Controller {

    /** Coordinates validated, cached searches. */
    private final SearchService searches;

    /**
     * Creates the application controller for Play dependency injection.
     *
     * @param searches the asynchronous search service
     * @author Nabeel Ali
     */
    @Inject
    public ApplicationController(SearchService searches) {
        this.searches = searches;
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
                .thenApply(page -> ok(views.html.results.render(page)))
                .exceptionally(failure -> searchError(hashtag, failure));
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
