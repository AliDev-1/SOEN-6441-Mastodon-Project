package services;

import java.time.Instant;
import java.util.List;
import models.Post;
import models.Readability;
import models.SearchPage;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies formulas with hand-counted examples and documented text partitions.
 * <p>AI assistance: test-design support for Phase 8 readability calculations.</p>
 * @author Nabeel Ali
 */
public class ReadabilityServiceTest {
    /** Stateless service under test. */
    private final ReadabilityService service = new ReadabilityService();

    /** Creates the test fixture.
     * @author Nabeel Ali
     */
    public ReadabilityServiceTest() { }

    /** Verifies an independent six-word example, retaining negative and high scores.
     * @author Nabeel Ali
     */
    @Test
    public void calculatesExactScores() {
        Readability score = service.analyze(post("one", "The cat sat on the mat.", "en"));
        assertTrue(score.isAvailable());
        assertEquals(6, score.getWords());
        assertEquals(1, score.getSentences());
        assertEquals(6, score.getSyllables());
        assertEquals(116.145, score.getReadingEase(), 0.000001);
        assertEquals(-1.45, score.getGradeLevel(), 0.000001);
        assertEquals("English estimate using approximate syllable counts.", score.getNote());
        Readability split = service.analyze(post("two", "The cat sat. A dog ran!", "en"));
        assertEquals(2, split.getSentences());
        assertEquals(119.19, split.getReadingEase(), 0.000001);
        assertEquals(-2.62, split.getGradeLevel(), 0.000001);
    }

    /** Verifies multi-syllable words and floating-point ratios before display rounding.
     * @author Nabeel Ali
     */
    @Test
    public void countsSyllablesWithDocumentedHeuristic() {
        Readability score = service.analyze(post("one", "A simple sentence.", "en"));
        assertEquals(3, score.getWords());
        assertEquals(5, score.getSyllables());
        assertEquals(62.79, score.getReadingEase(), 0.000001);
        assertEquals(5.2466666667, score.getGradeLevel(), 0.000001);
        // Covers silent e, consonant-le, vowel groups, y, accents, and no-vowel fallback.
        Readability words = service.analyze(post("two", "make table queue rhythm tsk cát banana", "en"));
        assertEquals(7, words.getWords());
        assertEquals(10, words.getSyllables());
        assertEquals(1, words.getSentences());
    }

    /** Checks URL exclusion, apostrophes, hyphens, punctuation and trailing fragments.
     * @author Nabeel Ali
     */
    @Test
    public void handlesSocialPostText() {
        Readability score = service.analyze(post("one", "Can't stop?! ... Don't go https://example.org/a.b www.example.org 123 😀", "en"));
        assertEquals(4, score.getWords());
        assertEquals(2, score.getSentences());
        assertEquals(4, score.getSyllables());
        Readability tags = service.analyze(post("two", "#cat @dog cat-dog cat’s", "en"));
        assertEquals(5, tags.getWords());
        assertEquals(1, tags.getSentences());
        assertEquals(5, tags.getSyllables());
    }

    /** Checks absent analyzable text without NaN, infinities or displayed scores.
     * @author Nabeel Ali
     */
    @Test
    public void rejectsTextWithoutWords() {
        for (String text : List.of("", "  ", "!!!", "123 😀", "https://example.org/path")) {
            Readability score = service.analyze(post("one", text, "en"));
            assertFalse(score.isAvailable());
            assertEquals(0, score.getWords());
            assertEquals(0, score.getSentences());
            assertEquals(0, score.getSyllables());
            assertEquals(0.0, score.getReadingEase(), 0.0);
            assertEquals(0.0, score.getGradeLevel(), 0.0);
            assertEquals("Readability unavailable: no words to analyze.", score.getNote());
        }
    }

    /** Checks declared English, regional English, unknown and unsupported languages.
     * @author Nabeel Ali
     */
    @Test
    public void respectsLanguageMetadata() {
        assertTrue(service.analyze(post("one", "Cat", "EN-us")).isAvailable());
        Readability unknown = service.analyze(post("one", "Cat", "Unknown"));
        assertTrue(unknown.isAvailable());
        assertEquals("English-based estimate; the post language is unknown.", unknown.getNote());
        for (String language : List.of("fr", "ja", "english", "")) {
            Readability score = service.analyze(post("one", "Bonjour", language));
            assertFalse(score.isAvailable());
            assertEquals("Readability unavailable: these formulas estimate English text only.", score.getNote());
        }
    }

    /** Checks per-post lookup, immutable score maps and missing-post errors.
     * @author Nabeel Ali
     */
    @Test
    public void analyzesOnlyPostsInSnapshot() {
        Post first = post("opaque:first", "Cat", "en");
        Post second = post("opaque:second", "Dog", "Unknown");
        SearchPage page = new SearchPage("snapshot", "java", "", List.of(first, second));
        var scores = service.analyzePage(page);
        assertEquals(2, scores.size());
        assertEquals(1, scores.get(first.getId()).getWords());
        assertTrue(scores.get(second.getId()).getNote().contains("unknown"));
        assertThrows(UnsupportedOperationException.class, scores::clear);
        assertSame(second, service.findPost(page, second.getId()));
        assertEquals(404, assertThrows(MastodonException.class, () -> service.findPost(page, "missing")).getStatus());
        SearchPage empty = new SearchPage("empty", "java", "", List.of());
        assertTrue(service.analyzePage(empty).isEmpty());
        assertEquals(404, assertThrows(MastodonException.class, () -> service.findPost(empty, "missing")).getStatus());
    }

    /** Creates synthetic posts without JSON or HTTP dependencies.
     * @param id the opaque identifier
     * @param text the readable content
     * @param language the declared language
     * @return a test post
     * @author Nabeel Ali
     */
    private static Post post(String id, String text, String language) {
        return new Post(id, "Author", "author", Instant.EPOCH, language, text, "https://example.org/post", "public");
    }
}
