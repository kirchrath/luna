package de.kirchrath.luna.mail.services;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** Turns the HTML of an email into readable text with line breaks. */
@Component
public class HtmlTextConverter {

    private static final String LINE_BREAK = "\n";
    private static final String LINE_BREAKING_ELEMENTS = "p, div, li, tr, h1, h2, h3, h4, h5, h6";
    private static final String NO_BREAK_SPACE = " ";
    private static final String SPACE = " ";
    private static final String TRAILING_SPACES = "[ \\t\\x0B\\f\\r]+\\n";
    private static final String BLANK_LINES = "\\n{3,}";
    private static final String ONE_BLANK_LINE = "\n\n";

    public String toText(String html) {
        var document = Jsoup.parse(html);
        document.select(LINE_BREAKING_ELEMENTS).after(LINE_BREAK);
        return document.body().wholeText()
                .replace(NO_BREAK_SPACE, SPACE)
                .replaceAll(TRAILING_SPACES, LINE_BREAK)
                .replaceAll(BLANK_LINES, ONE_BLANK_LINE)
                .strip();
    }
}
