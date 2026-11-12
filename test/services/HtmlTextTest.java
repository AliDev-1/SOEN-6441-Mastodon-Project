package services;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 * Exercises text conversion partitions with exact expected strings.
 *
 * <p>AI assistance: test-design support for Phase 3 HTML conversion edge cases.</p>
 * @author Nabeel Ali
 */
@RunWith(Parameterized.class)
public class HtmlTextTest {
    /** The HTML input for this case. */
    private final String input;
    /** The independently specified expected text. */
    private final String expected;

    /**
     * Stores one conversion case.
     * @param input the HTML fragment
     * @param expected the readable output
     * @author Nabeel Ali
     */
    public HtmlTextTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    /**
     * Supplies empty, ordinary, structured, encoded, and unsafe-content partitions.
     * @return paired inputs and expected outputs
     * @author Nabeel Ali
     */
    @Parameterized.Parameters(name = "case {index}")
    public static Collection<Object[]> cases() {
        // Literal expectations avoid computing the answer with the converter itself.
        return Arrays.asList(new Object[][] {
            {null, ""},
            {"", ""},
            {"  \n\t  ", ""},
            {"Hello Java", "Hello Java"},
            {"<p>Hello <strong>Java</strong>!</p><p>Next<br>line.</p>", "Hello Java! Next line."},
            {"<p>A &amp; B&nbsp;&lt;3 &#128522;</p>", "A & B <3 😊"},
            {"<p>Café 日本語 😊</p>", "Café 日本語 😊"},
            {"<p>Read <a href='https://example.org'>this link</a>.</p>", "Read this link."},
            {"<script>alert(1)</script><style>p{color:red}</style><p onclick='bad()'>Safe</p>", "Safe"},
            {"<p>Hello <b>world", "Hello world"},
            {"<img src='https://example.org/image.png'>", ""},
            {"&lt;script&gt;literal&lt;/script&gt;", "<script>literal</script>"}
        });
    }

    /**
     * Checks the complete normalized text for this input.
     * @author Nabeel Ali
     */
    @Test
    public void convertsToExpectedText() {
        assertEquals(expected, new HtmlText().toPlainText(input));
    }
}
