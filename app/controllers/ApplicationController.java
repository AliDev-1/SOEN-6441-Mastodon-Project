package controllers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * Handles browser requests and delegates application work to services.
 *
 * <p>AI assistance: implementation support for the Phase 2 request for one
 * asynchronous controller and a tested home page.</p>
 *
 * @author Nabeel Ali
 */
public class ApplicationController extends Controller {

    /**
     * Creates the application controller for Play dependency injection.
     *
     * @author Nabeel Ali
     */
    public ApplicationController() {
        // No service dependencies are needed for the initial home page.
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
}
