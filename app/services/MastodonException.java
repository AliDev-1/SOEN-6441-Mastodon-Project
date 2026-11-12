package services;

/**
 * Describes a safe application error without exposing tokens or upstream bodies.
 *
 * <p>AI assistance: implementation support for Phase 4 error handling.</p>
 * @author Nabeel Ali
 */
public final class MastodonException extends RuntimeException {
    /** The HTTP status the application should return to its caller. */
    private final int status;

    /**
     * Creates an error with a controlled, user-readable message.
     * @param status the application HTTP status
     * @param message the safe explanation
     * @author Nabeel Ali
     */
    public MastodonException(int status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Returns the status for the future controller error response.
     * @return the application HTTP status
     * @author Nabeel Ali
     */
    public int getStatus() {
        return status;
    }
}
