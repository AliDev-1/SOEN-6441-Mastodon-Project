package services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Converts post HTML into normalized readable text without fetching resources.
 *
 * <p>AI assistance: implementation support for Phase 3 HTML conversion.</p>
 *
 * @author Nabeel Ali
 */
public final class HtmlText {
    /**
     * Creates a stateless text converter.
     * @author Nabeel Ali
     */
    public HtmlText() {
        // All parsing state belongs to an individual conversion.
    }

    /**
     * Removes markup and executable content, decodes entities, and separates blocks.
     * The returned text must still be escaped when inserted into an HTML page.
     *
     * @param html the content fragment, or null when no text is available
     * @return normalized plain text, or an empty string for null content
     * @author Nabeel Ali
     */
    public String toPlainText(String html) {
        // Handle absent content independently from API field validation.
        if (html == null) {
            return "";
        }
        // Parse locally; scripts and styles contribute no readable post text.
        Document document = Jsoup.parseBodyFragment(html);
        document.select("script, style").remove();
        return document.body().text();
    }
}
