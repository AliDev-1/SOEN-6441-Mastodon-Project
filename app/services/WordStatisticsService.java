package services;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import models.Post;
import models.SearchPage;
import models.WordFrequency;
import models.WordStatistics;

/**
 * Uses Java Streams to count normalized words from the displayed cached page.
 * <p>AI assistance: implementation support for Phase 9 tokenization and aggregation.</p>
 * @author Nabeel Ali
 */
public final class WordStatisticsService {
    /** Unicode letter words with combining marks and optional internal apostrophes. */
    private static final Pattern WORD = Pattern.compile("\\p{L}[\\p{L}\\p{M}]*(?:'\\p{L}[\\p{L}\\p{M}]*)*");
    /** Excludes complete HTTP(S) and www URLs before extracting words. */
    private static final Pattern URL = Pattern.compile("(?i)https?://\\S+|www\\.\\S+");

    /** Creates the stateless analyzer.
     * @author Nabeel Ali
     */
    public WordStatisticsService() { }

    /**
     * Counts all words across this page only, retaining stop words and all languages.
     * @param page the cached snapshot being viewed
     * @return frequencies sorted by descending count, then natural word order
     * @author Nabeel Ali
     */
    public WordStatistics analyze(SearchPage page) {
        // Flatten each post's words, then group identical normalized spellings.
        var counts = page.getPosts().stream()
                .map(Post::getContent)
                .flatMap(this::words)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // Explicit tie-breaking makes equal-frequency results deterministic.
        var frequencies = counts.entrySet().stream()
                .map(entry -> new WordFrequency(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(WordFrequency::getCount).reversed()
                        .thenComparing(WordFrequency::getWord))
                .toList();
        return new WordStatistics(frequencies);
    }

    /**
     * Normalizes case and Unicode while excluding URL text and nonword characters.
     * @param text the already-readable post content
     * @return a stream of normalized words
     * @author Nabeel Ali
     */
    private Stream<String> words(String text) {
        // Normalize curly apostrophes together; retain accents as part of each word.
        String cleaned = URL.matcher(text).replaceAll(" ").replace('’', '\'');
        String normalized = Normalizer.normalize(cleaned.toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
        return WORD.matcher(normalized).results().map(MatchResult::group);
    }
}
