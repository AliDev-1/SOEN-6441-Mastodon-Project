package services;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Verifies the safe error values consumed by future controller actions.
 * <p>AI assistance: test-design support for Phase 4 controlled errors.</p>
 * @author Nabeel Ali
 */
public class MastodonExceptionTest {
    /**
     * Creates the JUnit fixture.
     * @author Nabeel Ali
     */
    public MastodonExceptionTest() {
        // No external state is required.
    }

    /**
     * Preserves the chosen status and message without attaching raw failure data.
     * @author Nabeel Ali
     */
    @Test
    public void preservesSafeErrorValues() {
        MastodonException error = new MastodonException(503, "Service unavailable.");
        assertEquals(503, error.getStatus());
        assertEquals("Service unavailable.", error.getMessage());
        assertNull(error.getCause());
    }
}
