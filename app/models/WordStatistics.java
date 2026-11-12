package models;

import java.util.List;

/**
 * Immutable ordered frequencies and summary counts for one cached page.
 * <p>AI assistance: implementation support for Phase 9 statistics values.</p>
 * @author Nabeel Ali
 */
public final class WordStatistics {
    /** Frequencies ordered by count, then word. */
    private final List<WordFrequency> frequencies;

    /** Copies the ordered frequencies so callers cannot change shared results.
     * @param frequencies the ordered entries
     * @author Nabeel Ali
     */
    public WordStatistics(List<WordFrequency> frequencies) {
        this.frequencies = List.copyOf(frequencies);
    }

    /** Returns the ordered frequency entries.
     * @return the unmodifiable entries
     * @author Nabeel Ali
     */
    public List<WordFrequency> getFrequencies() { return frequencies; }

    /** Returns the number of distinct normalized words.
     * @return the unique-word count
     * @author Nabeel Ali
     */
    public int getUniqueWords() { return frequencies.size(); }

    /** Sums all occurrences, including repeated words.
     * @return the total word count
     * @author Nabeel Ali
     */
    public long getTotalWords() {
        return frequencies.stream().mapToLong(WordFrequency::getCount).sum();
    }
}
