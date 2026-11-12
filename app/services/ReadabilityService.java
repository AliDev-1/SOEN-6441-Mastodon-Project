package services;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import models.Post;
import models.Readability;
import models.SearchPage;

/**
 * Estimates English readability from cached plain text without any external calls.
 * <p>AI assistance: implementation support for Phase 8 formulas and text heuristics.</p>
 * @author Nabeel Ali
 */
public final class ReadabilityService {
    /** Counts letter sequences while preserving internal apostrophes. */
    private static final Pattern WORD = Pattern.compile("\\p{L}+(?:['’]\\p{L}+)*");
    /** Approximates syllables by groups of English vowels, including y. */
    private static final Pattern VOWELS = Pattern.compile("[aeiouy]+");

    /** Creates the stateless calculator.
     * @author Nabeel Ali
     */
    public ReadabilityService() { }

    /**
     * Calculates scores for each cached result without changing its posts.
     * @param page the displayed snapshot
     * @return scores indexed by opaque post ID
     * @author Nabeel Ali
     */
    public Map<String, Readability> analyzePage(SearchPage page) {
        Map<String, Readability> scores = new LinkedHashMap<>();
        for (Post post : page.getPosts()) {
            scores.put(post.getId(), analyze(post));
        }
        return Map.copyOf(scores);
    }

    /**
     * Finds a post only within the requested cached snapshot.
     * @param page the retained page
     * @param postId the requested opaque post ID
     * @return the matching cached post
     * @throws MastodonException when the post is not in this page
     * @author Nabeel Ali
     */
    public Post findPost(SearchPage page, String postId) {
        for (Post post : page.getPosts()) {
            if (post.getId().equals(postId)) {
                return post;
            }
        }
        throw new MastodonException(404, "This post is not part of the saved results. Please return to your search.");
    }

    /**
     * Calculates Flesch scores using documented word, sentence and syllable heuristics.
     * @param post the plain-text post and its declared language
     * @return scores and counts, or an explanation of unavailability
     * @author Nabeel Ali
     */
    public Readability analyze(Post post) {
        String language = post.getLanguage().toLowerCase(Locale.ROOT);
        boolean unknown = language.equals("unknown");
        if (!unknown && !language.equals("en") && !language.startsWith("en-")) {
            return unavailable("Readability unavailable: these formulas estimate English text only.");
        }

        // Exclude URLs before splitting punctuation; keep words from the actual post.
        String text = post.getContent().replaceAll("(?i)https?://\\S+|www\\.\\S+", " ");
        int words = 0;
        int sentences = 0;
        int syllables = 0;
        for (String sentence : text.split("[.!?]+")) {
            Matcher matcher = WORD.matcher(sentence);
            int sentenceWords = 0;
            while (matcher.find()) {
                sentenceWords++;
                syllables += syllables(matcher.group());
            }
            // A punctuation-free fragment counts as one sentence if it has words.
            if (sentenceWords > 0) {
                sentences++;
                words += sentenceWords;
            }
        }
        if (words == 0) {
            return unavailable("Readability unavailable: no words to analyze.");
        }

        // Use floating-point ratios and retain full precision until display.
        double wordsPerSentence = (double) words / sentences;
        double syllablesPerWord = (double) syllables / words;
        double ease = 206.835 - 1.015 * wordsPerSentence - 84.6 * syllablesPerWord;
        double grade = 0.39 * wordsPerSentence + 11.8 * syllablesPerWord - 15.59;
        String note = unknown
                ? "English-based estimate; the post language is unknown."
                : "English estimate using approximate syllable counts.";
        return new Readability(words, sentences, syllables, ease, grade, note);
    }

    /**
     * Counts vowel groups, adjusts silent e, and keeps at least one syllable per word.
     * @param input one matched word
     * @return its approximate syllable count
     * @author Nabeel Ali
     */
    private int syllables(String input) {
        // Fold accents and remove apostrophes before applying the English heuristic.
        String word = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("[^a-z]", "");
        int count = (int) VOWELS.matcher(word).results().count();
        if (count > 1 && word.endsWith("e") && !word.matches(".*[^aeiouy]le$")) {
            count--;
        }
        return Math.max(1, count);
    }

    /**
     * Creates an unavailable result without divisions by zero or misleading scores.
     * @param note the user-readable reason
     * @return a result with no analyzed words
     * @author Nabeel Ali
     */
    private Readability unavailable(String note) {
        return new Readability(0, 0, 0, 0, 0, note);
    }
}
