package models;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies the immutable readability values exposed to templates.
 * <p>AI assistance: test-design support for Phase 8 result values.</p>
 * @author Nabeel Ali
 */
public class ReadabilityTest {
    /** Creates the fixture.
     * @author Nabeel Ali
     */
    public ReadabilityTest() { }

    /** Checks exact field values and score availability.
     * @author Nabeel Ali
     */
    @Test
    public void exposesValues() {
        Readability score = new Readability(6, 1, 6, 116.145, -1.45, "Estimate");
        assertEquals(6, score.getWords());
        assertEquals(1, score.getSentences());
        assertEquals(6, score.getSyllables());
        assertEquals(116.145, score.getReadingEase(), 0.0);
        assertEquals(-1.45, score.getGradeLevel(), 0.0);
        assertEquals("Estimate", score.getNote());
        assertTrue(score.isAvailable());
        assertFalse(new Readability(0, 0, 0, 0, 0, "Unavailable").isAvailable());
    }
}
