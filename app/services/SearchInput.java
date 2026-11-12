package services;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Validates and normalizes browser search parameters before any API request.
 * <p>AI assistance: implementation support for Phase 5 input validation.</p>
 * @author Nabeel Ali
 */
public final class SearchInput {
    /** Creates the stateless validator.
     * @author Nabeel Ali
     */
    public SearchInput() { }

    /**
     * Normalizes a hashtag without a leading hash character.
     * @param input the browser value
     * @return the normalized hashtag
     * @throws MastodonException for invalid input
     * @author Nabeel Ali
     */
    public String hashtag(String input) {
        // Normalize equivalent Unicode spellings and case for shared cache keys.
        String tag = Normalizer.normalize(input == null ? "" : input.strip(),
                Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        if (!tag.matches("[\\p{L}\\p{M}\\p{N}_]{1,100}")
                || !tag.matches(".*\\p{L}.*")) {
            throw new MastodonException(400,
                    "Enter a hashtag without #, using 1–100 letters, numbers, or underscores and at least one letter.");
        }
        return tag;
    }

    /**
     * Validates an opaque pagination cursor without converting it to a number.
     * @param input the optional browser value
     * @return a trimmed cursor, or an empty string for the first page
     * @throws MastodonException for invalid input
     * @author Nabeel Ali
     */
    public String cursor(String input) {
        String cursor = input == null ? "" : input.strip();
        // Permit opaque punctuation but reject separators, control characters and huge values.
        if (!cursor.matches("[^\\p{Z}\\p{C}\\s]{0,256}")) {
            throw new MastodonException(400, "Invalid pagination cursor. Please search again.");
        }
        return cursor;
    }
}
