package com.autonews;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RssReader {

    public record NewsItem(String title, String url, String description) {}

    public List<NewsItem> fetchItems(String feedUrl) {
        List<NewsItem> items = new ArrayList<>();
        try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(new URL(feedUrl)));

            for (SyndEntry entry : feed.getEntries()) {
                String title = entry.getTitle() != null ? entry.getTitle().trim() : "";
                String link  = entry.getLink()  != null ? entry.getLink().trim()  : "";
                String desc  = "";
                if (entry.getDescription() != null) {
                    desc = entry.getDescription().getValue()
                               .replaceAll("<[^>]+>", " ").trim();
                }
                if (!title.isEmpty() && !link.isEmpty()) {
                    items.add(new NewsItem(title, link, desc));
                }
            }
        } catch (Exception e) {
            System.err.println("[RssReader] Error en " + feedUrl + ": " + e.getMessage());
        }
        return items;
    }
}
