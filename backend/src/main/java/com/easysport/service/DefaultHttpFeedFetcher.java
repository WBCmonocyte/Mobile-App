package com.easysport.service;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

@Component
public class DefaultHttpFeedFetcher implements FeedFetcher {

    public static final String VNEXPRESS_FEED = "https://vnexpress.net/rss/the-thao.rss";
    public static final String TUOITRE_FEED = "https://tuoitre.vn/rss/the-thao.rss";

    private static final Set<String> ALLOWED_FEEDS = Set.of(VNEXPRESS_FEED, TUOITRE_FEED);

    private final HttpClient httpClient;

    public DefaultHttpFeedFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public DefaultHttpFeedFetcher(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String fetch(String url) throws Exception {
        if (!ALLOWED_FEEDS.contains(url)) {
            throw new IllegalArgumentException("Nguồn tin không nằm trong danh sách được cấu hình: " + url);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 EasySport/1.0")
                .header("Accept", "application/rss+xml, application/xml, text/xml; q=0.9, */*; q=0.8")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " khi tải feed: " + url);
        }

        return response.body();
    }
}
