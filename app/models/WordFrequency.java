package models;

/**
 * One normalized word and its number of occurrences on a cached page.
 * <p>AI assistance: implementation support for Phase 9 frequency values.</p>
 * @author Nabeel Ali
 */
public final class WordFrequency {
    /** The normalized word. */
    private final String word;
    /** The number of occurrences. */
    private final long count;

    /** Creates one frequency entry.
     * @param word the normalized word
     * @param count its occurrence count
     * @author Nabeel Ali
     */
    public WordFrequency(String word, long count) {
        this.word = word;
        this.count = count;
    }

    /** Returns the normalized word.
     * @return the word
     * @author Nabeel Ali
     */
    public String getWord() { return word; }

    /** Returns its frequency.
     * @return the occurrence count
     * @author Nabeel Ali
     */
    public long getCount() { return count; }
}
