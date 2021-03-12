package core.emoji;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author udhansingh
 *
 */
public class EmojiUnicodePointAndValueMaker {
    // 1 Minute
    static final Integer timeout = 60000;

    // 100 MB limit
    static final int maxBodySize = 1024 * 1024 * 100;

    public List<UnicodePointEntry> build(String unicodeListingUrl) throws IOException {
        final Document document = Jsoup.connect(unicodeListingUrl)
                .timeout(timeout)
                .maxBodySize(maxBodySize)
                .userAgent("Mozilla/5.0 (Windows NT 6.1; rv:19.0) Gecko/20100101 Firefox/19.0")
                .get();

        // Document must be valid
        assert(document != null);

        // Select all <tr> elements from the HTML page
        final Elements trElements = document.select("tr");

        // Track code point entries
        final List<UnicodePointEntry> entries = new ArrayList<>();

        // Use an iterator to traverse the list
        final Iterator<Element> trElementIterator = trElements.listIterator();

        // Track row count
        int row = 1;
        while (trElementIterator.hasNext()) {
            final Element trElement = trElementIterator.next();

            try {
                // Extract code part
                final Elements tdNameElements = trElement.select("td[class=name]");
                final Elements tdCodeElements = trElement.select("td[class=code]");

                final String name = tdNameElements.text().trim();
                final String codes = tdCodeElements.text().trim();

                if(!name.isEmpty() && !codes.isEmpty()) {
                    // Advance only if values are extracted
                    final UnicodePointEntry entry = new UnicodePointEntry(
                            row,
                            name,
                            codes
                    );

                    entries.add(entry);

                    // Move row ahead
                    row++;
                }
            } catch(Throwable t) {
                // Ignore exceptions
            }
        }

        return entries;
    }
}
