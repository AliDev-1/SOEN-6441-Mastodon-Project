package models;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies frequency values and immutable statistics summaries.
 * <p>AI assistance: test-design support for Phase 9 statistics models.</p>
 * @author Nabeel Ali
 */
public class WordStatisticsTest {
    /** Creates the test fixture.
     * @author Nabeel Ali
     */
    public WordStatisticsTest() { }

    /** Checks getters, defensive copying, totals and unmodifiable output.
     * @author Nabeel Ali
     */
    @Test
    public void preservesValues() {
        WordFrequency entry = new WordFrequency("cat", 3);
        assertEquals("cat", entry.getWord());
        assertEquals(3, entry.getCount());
        List<WordFrequency> source = new ArrayList<>(List.of(entry, new WordFrequency("dog", 2)));
        WordStatistics statistics = new WordStatistics(source);
        source.clear();
        assertSame(entry, statistics.getFrequencies().getFirst());
        assertEquals(2, statistics.getUniqueWords());
        assertEquals(5, statistics.getTotalWords());
        assertThrows(UnsupportedOperationException.class, () -> statistics.getFrequencies().clear());
        assertEquals(0, new WordStatistics(List.of()).getTotalWords());
    }
}
