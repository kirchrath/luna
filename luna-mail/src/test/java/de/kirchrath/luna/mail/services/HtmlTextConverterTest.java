package de.kirchrath.luna.mail.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlTextConverterTest {

    private final HtmlTextConverter htmlTextConverter = new HtmlTextConverter();

    @Test
    void thatConvertingHtmlShouldResultIntoTextWithLineBreaksAndWithoutMarkup() {
        // given
        var html = """
                <html><head><style>p { color: red; }</style></head>
                <body><h1>Invoice</h1><p>Dear&nbsp;customer,</p><p>please pay <b>42 €</b>.<br>Thank you</p>
                <div></div><div></div><div></div><ul><li>Item 1</li><li>Item 2</li></ul>
                <script>alert('x');</script></body></html>""";

        // when
        var text = htmlTextConverter.toText(html);

        // then
        assertThat(text).isEqualTo("Invoice\nDear customer,\nplease pay 42 €.\nThank you\n\nItem 1\nItem 2");
    }
}
