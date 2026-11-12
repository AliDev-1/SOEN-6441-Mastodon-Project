package services;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import models.Post;
import models.SearchPage;
import models.WordFrequency;
import models.WordStatistics;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Checks exact frequencies, normalization and deterministic ordering.
 * <p>AI assistance: test-design support for Phase 9 Java Streams analysis.</p>
 * @author Nabeel Ali
 */
public class WordStatisticsServiceTest {
    /** The stateless analyzer under test. */
    private final WordStatisticsService service = new WordStatisticsService();

    /** Creates the test fixture.
     * @author Nabeel Ali
     */
    public WordStatisticsServiceTest() { }

    /** Verifies aggregation across posts, stop-word retention and alphabetical ties.
     * @author Nabeel Ali
     */
    @Test
    public void aggregatesAndSortsWords() {
        WordStatistics result = analyze("Banana apple APPLE the", "banana cherry the apple");
        assertEquals(List.of("apple:3", "banana:2", "the:2", "cherry:1"), entries(result));
        assertEquals(8, result.getTotalWords());
        assertEquals(4, result.getUniqueWords());
    }

    /** Verifies canonical Unicode forms, retained accents and internal apostrophes.
     * @author Nabeel Ali
     */
    @Test
    public void normalizesUnicodeAndApostrophes() {
        WordStatistics result = analyze("CAFÉ cafe\u0301 cafe CAN’T can't 'hello' hello 東京 東京");
        assertEquals(List.of("café:2", "can't:2", "hello:2", "東京:2", "cafe:1"), entries(result));
        assertEquals(9, result.getTotalWords());
    }

    /** Verifies URL, number and emoji exclusion plus separator and marker handling.
     * @author Nabeel Ali
     */
    @Test
    public void handlesSocialText() {
        WordStatistics result = analyze("Cat #cat @dog cat-dog https://example.org/hidden WWW.example.org 123 😀");
        assertEquals(List.of("cat:3", "dog:2"), entries(result));
        assertEquals(List.of("a:1", "b:1", "c:1"), entries(analyze("a1b_c")));
    }

    /** Checks empty pages and text without words produce zero rather than an error.
     * @author Nabeel Ali
     */
    @Test
    public void supportsEmptyResults() {
        for (WordStatistics result : List.of(analyze(), analyze("", "  ", "123 😀 !!!", "https://example.org"))) {
            assertEquals(0, result.getTotalWords());
            assertEquals(0, result.getUniqueWords());
            assertTrue(result.getFrequencies().isEmpty());
        }
        assertThrows(UnsupportedOperationException.class, () -> analyze("cat").getFrequencies().clear());
    }

    /** Checks no counts leak across page analyses or service invocations.
     * @author Nabeel Ali
     */
    @Test
    public void limitsAnalysisToSuppliedPage() {
        assertEquals(List.of("cat:2"), entries(analyze("cat cat")));
        assertEquals(List.of("dog:1"), entries(analyze("dog")));
        assertEquals(List.of("cat:1"), entries(analyze("cat")));
    }

    /** Builds synthetic cached posts; all language labels are intentionally unknown.
     * @param contents the post contents
     * @return the computed statistics
     * @author Nabeel Ali
     */
    private WordStatistics analyze(String... contents) {
        List<Post> posts = Arrays.stream(contents).map(text -> new Post(text, "Author", "author",
                Instant.EPOCH, "Unknown", text, "https://example.org/post", "public")).toList();
        return service.analyze(new SearchPage("snapshot", "java", "", posts));
    }

    /** Converts output entries to an easily compared ordered list.
     * @param statistics the computed result
     * @return ordered word/count pairs
     * @author Nabeel Ali
     */
    private List<String> entries(WordStatistics statistics) {
        return statistics.getFrequencies().stream().map(entry -> entry.getWord() + ":" + entry.getCount()).toList();
    }
}
