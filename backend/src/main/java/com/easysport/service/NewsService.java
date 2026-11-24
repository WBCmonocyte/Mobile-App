package com.easysport.service;

import com.easysport.dto.NewsItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    public static final String VNEXPRESS_FEED = "https://vnexpress.net/rss/the-thao.rss";
    public static final String TUOITRE_FEED = "https://tuoitre.vn/rss/the-thao.rss";

    private static final Duration CACHE_DURATION = Duration.ofMinutes(15);
    private static final int MAX_NEWS_ITEMS = 20;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final FeedFetcher feedFetcher;
    private final Clock clock;

    private List<NewsItemDto> memoryCache = null;
    private Instant cacheExpiresAt = null;

    public NewsService(FeedFetcher feedFetcher, Clock clock) {
        this.feedFetcher = feedFetcher;
        this.clock = clock;
    }

    public synchronized List<NewsItemDto> getLatestNews() {
        Instant now = clock.instant();

        // 1. Kiểm tra cache còn hạn (15 phút)
        if (memoryCache != null && cacheExpiresAt != null && now.isBefore(cacheExpiresAt)) {
            return memoryCache;
        }

        // 2. Tải các feed được cấu hình độc lập
        List<NewsItemDto> fetchedItems = new ArrayList<>();
        boolean atLeastOneSuccess = false;

        // Feed 1: VnExpress
        try {
            String xml = feedFetcher.fetch(VNEXPRESS_FEED);
            List<NewsItemDto> vneItems = parseFeedXml(xml, "VnExpress");
            fetchedItems.addAll(vneItems);
            atLeastOneSuccess = true;
        } catch (Exception e) {
            log.warn("Lỗi khi tải feed VnExpress: {}", e.getMessage());
        }

        // Feed 2: Tuổi Trẻ
        try {
            String xml = feedFetcher.fetch(TUOITRE_FEED);
            List<NewsItemDto> ttItems = parseFeedXml(xml, "Tuổi Trẻ");
            fetchedItems.addAll(ttItems);
            atLeastOneSuccess = true;
        } catch (Exception e) {
            log.warn("Lỗi khi tải feed Tuổi Trẻ: {}", e.getMessage());
        }

        // 3. Xử lý kết quả tải
        if (atLeastOneSuccess) {
            // Loại trùng theo URL (chuẩn hóa), sắp xếp mới nhất trước, giới hạn tối đa 20 tin
            List<NewsItemDto> processed = processAndDeduplicate(fetchedItems);
            this.memoryCache = processed;
            this.cacheExpiresAt = now.plus(CACHE_DURATION);
            return processed;
        }

        // 4. Nếu tất cả nguồn đều lỗi: dùng bản cache gần nhất nếu có
        if (this.memoryCache != null) {
            log.warn("Tất cả nguồn RSS đều lỗi, sử dụng bản cache gần nhất.");
            return this.memoryCache;
        }

        // Nếu chưa có cache thì trả lỗi rõ ràng
        throw new IllegalStateException("Không thể tải tin tức thể thao lúc này. Vui lòng thử lại sau.");
    }

    /**
     * Parse XML RSS sử dụng trình đọc bảo mật (tắt DTD và external entities).
     */
    public List<NewsItemDto> parseFeedXml(String xml, String sourceName) throws Exception {
        if (xml == null || xml.isBlank()) {
            return Collections.emptyList();
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        try { dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); } catch (Exception ignored) {}
        try { dbf.setFeature("http://xml.org/sax/features/external-general-entities", false); } catch (Exception ignored) {}
        try { dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false); } catch (Exception ignored) {}
        try { dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); } catch (Exception ignored) {}
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(xml)));
        doc.getDocumentElement().normalize();

        NodeList itemNodes = doc.getElementsByTagName("item");
        List<NewsItemDto> list = new ArrayList<>();

        for (int i = 0; i < itemNodes.getLength(); i++) {
            Element el = (Element) itemNodes.item(i);
            String title = getElementText(el, "title");
            String link = getElementText(el, "link");
            String pubDate = getElementText(el, "pubDate");

            // Bỏ bản ghi thiếu tiêu đề hoặc link
            if (title == null || title.isBlank() || link == null || link.isBlank()) {
                continue;
            }

            title = cleanTitle(title);
            link = link.trim();

            OffsetDateTime odt = parsePubDate(pubDate);
            String publishedAtIso = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            list.add(new NewsItemDto(title, sourceName, publishedAtIso, link));
        }

        return list;
    }

    private String cleanTitle(String raw) {
        if (raw == null) return "";
        return raw.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("^<!\\[CDATA\\[", "")
                .replaceAll("\\]\\]>$", "");
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            return nl.item(0).getTextContent();
        }
        return null;
    }

    /**
     * Chuẩn hóa thời gian sang OffsetDateTime (Việt Nam +07:00 hoặc múi giờ trong feed).
     */
    public OffsetDateTime parsePubDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return OffsetDateTime.ofInstant(clock.instant(), VIETNAM_ZONE);
        }

        // Loại bỏ ký tự khoảng trắng đặc biệt như \u202f (narrow no-break space), \u00a0
        String cleaned = rawDate.replaceAll("[\\s\\u202f\\u00a0]+", " ").trim();

        // 1. RFC 1123 / RFC 822 (VnExpress: "Tue, 24 Nov 2026 09:25:52 +0700")
        try {
            return OffsetDateTime.parse(cleaned, DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (Exception ignored) {}

        try {
            DateTimeFormatter rfc822 = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            return OffsetDateTime.parse(cleaned, rfc822);
        } catch (Exception ignored) {}

        try {
            DateTimeFormatter rfc822Short = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
            return OffsetDateTime.parse(cleaned, rfc822Short);
        } catch (Exception ignored) {}

        // 2. ISO 8601 trực tiếp
        try {
            return OffsetDateTime.parse(cleaned);
        } catch (Exception ignored) {}

        // 3. Tuổi Trẻ dạng "11/24/2026 8:56:00 AM" hoặc "24/11/2026 08:56:00"
        String[] localPatterns = new String[] {
                "M/d/yyyy h:mm:ss a",
                "M/d/yyyy hh:mm:ss a",
                "MM/dd/yyyy h:mm:ss a",
                "MM/dd/yyyy hh:mm:ss a",
                "d/M/yyyy h:mm:ss a",
                "dd/MM/yyyy h:mm:ss a",
                "dd/MM/yyyy HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String pattern : localPatterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                LocalDateTime ldt = LocalDateTime.parse(cleaned, formatter);
                return ldt.atZone(VIETNAM_ZONE).toOffsetDateTime();
            } catch (Exception ignored) {}
        }

        // Mặc định trả về thời gian hiện tại theo clock
        return OffsetDateTime.ofInstant(clock.instant(), VIETNAM_ZONE);
    }

    private List<NewsItemDto> processAndDeduplicate(List<NewsItemDto> items) {
        // Loại trùng theo URL chuẩn hóa (loại bỏ query params/trailing slash không cần thiết)
        Map<String, NewsItemDto> uniqueByUrl = new LinkedHashMap<>();

        for (NewsItemDto item : items) {
            String normalizedUrl = normalizeUrl(item.getUrl());
            if (!uniqueByUrl.containsKey(normalizedUrl)) {
                uniqueByUrl.put(normalizedUrl, item);
            }
        }

        // Sắp xếp mới nhất trước
        return uniqueByUrl.values().stream()
                .sorted((a, b) -> {
                    OffsetDateTime dtA = parsePubDate(a.getPublishedAt());
                    OffsetDateTime dtB = parsePubDate(b.getPublishedAt());
                    return dtB.compareTo(dtA);
                })
                .limit(MAX_NEWS_ITEMS)
                .collect(Collectors.toList());
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        // Bỏ trailing slash nếu có
        if (trimmed.endsWith("/") && trimmed.length() > 8) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    // Helper cho kiểm thử hoặc xóa cache thủ công
    public synchronized void clearCache() {
        this.memoryCache = null;
        this.cacheExpiresAt = null;
    }

    public synchronized List<NewsItemDto> getMemoryCache() {
        return memoryCache;
    }
}
