package models;

/**
 * Immutable readability measurements for one post's plain text.
 * <p>AI assistance: implementation support for Phase 8 readability values.</p>
 * @author Nabeel Ali
 */
public final class Readability {
    /** Stores the counted words. */
    private final int words;
    /** Stores the counted sentences. */
    private final int sentences;
    /** Stores the estimated syllables. */
    private final int syllables;
    /** Stores the Flesch Reading Ease score. */
    private final double readingEase;
    /** Stores the Flesch–Kincaid Grade Level. */
    private final double gradeLevel;
    /** Stores the explanation of availability and estimation. */
    private final String note;

    /**
     * Creates a measurement; zero words means scores are unavailable.
     * @param words the counted words
     * @param sentences the counted sentences
     * @param syllables the estimated syllables
     * @param readingEase the Flesch Reading Ease score
     * @param gradeLevel the Flesch–Kincaid Grade Level
     * @param note the explanation of availability and estimation
     * @author Nabeel Ali
     */
    public Readability(int words, int sentences, int syllables, double readingEase, double gradeLevel, String note) {
        this.words = words;
        this.sentences = sentences;
        this.syllables = syllables;
        this.readingEase = readingEase;
        this.gradeLevel = gradeLevel;
        this.note = note;
    }

    /** Returns the counted words.
     * @return the counted words
     * @author Nabeel Ali
     */
    public int getWords() { return words; }

    /** Returns the counted sentences.
     * @return the counted sentences
     * @author Nabeel Ali
     */
    public int getSentences() { return sentences; }

    /** Returns the estimated syllables.
     * @return the estimated syllables
     * @author Nabeel Ali
     */
    public int getSyllables() { return syllables; }

    /** Returns the Flesch Reading Ease score.
     * @return the Flesch Reading Ease score
     * @author Nabeel Ali
     */
    public double getReadingEase() { return readingEase; }

    /** Returns the Flesch–Kincaid Grade Level.
     * @return the Flesch–Kincaid Grade Level
     * @author Nabeel Ali
     */
    public double getGradeLevel() { return gradeLevel; }

    /** Returns the explanation of availability and estimation.
     * @return the explanation of availability and estimation
     * @author Nabeel Ali
     */
    public String getNote() { return note; }

    /** Indicates whether enough supported text was available to calculate scores.
     * @return true when at least one word was counted
     * @author Nabeel Ali
     */
    public boolean isAvailable() { return words > 0; }
}
