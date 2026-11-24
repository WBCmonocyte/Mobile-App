package com.easysport;

import com.easysport.dto.NewsItemDto;
import com.easysport.service.FeedFetcher;
import com.easysport.service.NewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NewsServiceTest {

    private FeedFetcher feedFetcher;
    private MutableClock clock;
    private NewsService newsService;
    private Instant fixedInstant;

    static class MutableClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zone;

        public MutableClock(Instant start, ZoneId zone) {
            this.currentInstant = start;
            this.zone = zone;
        }

        public void advance(Duration duration) {
            this.currentInstant = this.currentInstant.plus(duration);
        }

        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return new MutableClock(currentInstant, zone); }
        @Override public Instant instant() { return currentInstant; }
    }

    private static final String SAMPLE_VNEXPRESS_XML = """
            <?xml version="1.0" encoding="utf-8"?>
            <rss version="2.0">
            <channel>
                <title>Thể thao - VnExpress</title>
                <item>
                    <title>Bài báo 1: Man Utd chiến thắng kịch tính</title>
                    <link>https://vnexpress.net/man-utd-chien-thang-1.html</link>
                    <pubDate>Tue, 24 Nov 2026 10:00:00 +0700</pubDate>
                </item>
                <item>
                    <title>Bài báo 2: Tuyển Việt Nam hội quân</title>
                    <link>https://vnexpress.net/tuyen-viet-nam-hoi-quan-2.html</link>
                    <pubDate>Tue, 24 Nov 2026 09:00:00 +0700</pubDate>
                </item>
                <item>
                    <title></title>
                    <link>https://vnexpress.net/bai-thieu-tieu-de.html</link>
                    <pubDate>Tue, 24 Nov 2026 08:00:00 +0700</pubDate>
                </item>
                <item>
                    <title>Bài thiếu link</title>
                    <link></link>
                    <pubDate>Tue, 24 Nov 2026 08:00:00 +0700</pubDate>
                </item>
            </channel>
            </rss>
            """;

    private static final String SAMPLE_TUOITRE_XML = """
            <?xml version="1.0" encoding="utf-8"?>
            <rss version="2.0">
            <channel>
                <title>Thể thao - Tuổi Trẻ</title>
                <item>
                    <title>Bài báo 3: U17 Việt Nam thăng hoa</title>
                    <link>https://tuoitre.vn/u17-viet-nam-thang-hoa-3.htm</link>
                    <pubDate>11/24/2026 9:30:00 AM</pubDate>
                </item>
                <item>
                    <title>Bài báo 1: Man Utd chiến thắng kịch tính (trùng link)</title>
                    <link>https://vnexpress.net/man-utd-chien-thang-1.html/</link>
                    <pubDate>11/24/2026 7:00:00 AM</pubDate>
                </item>
            </channel>
            </rss>
            """;

    @BeforeEach
    void setUp() {
        feedFetcher = mock(FeedFetcher.class);
        fixedInstant = Instant.parse("2026-11-24T10:00:00Z");
        clock = new MutableClock(fixedInstant, ZoneId.of("Asia/Ho_Chi_Minh"));
        newsService = new NewsService(feedFetcher, clock);
    }

    @Test
    @DisplayName("Tình huống có tin: Nạp thành công từ cả 2 nguồn, sắp xếp mới nhất trước, chuẩn hóa ISO 8601")
    void testGetLatestNews_successFromBothSources() throws Exception {
        when(feedFetcher.fetch(NewsService.VNEXPRESS_FEED)).thenReturn(SAMPLE_VNEXPRESS_XML);
        when(feedFetcher.fetch(NewsService.TUOITRE_FEED)).thenReturn(SAMPLE_TUOITRE_XML);

        List<NewsItemDto> news = newsService.getLatestNews();

        assertNotNull(news);
        // Có 4 bài hợp lệ ban đầu, nhưng có 1 bài trùng link -> còn 3 bài
        assertEquals(3, news.size());

        // Bài mới nhất là Bài 1 (10:00), tiếp theo Bài 3 (09:30), cuối cùng là Bài 2 (09:00)
        assertEquals("Bài báo 1: Man Utd chiến thắng kịch tính", news.get(0).getTitle());
        assertEquals("VnExpress", news.get(0).getSource());
        assertTrue(news.get(0).getPublishedAt().contains("2026-11-24T10:00:00"));

        assertEquals("Bài báo 3: U17 Việt Nam thăng hoa", news.get(1).getTitle());
        assertEquals("Tuổi Trẻ", news.get(1).getSource());
        assertTrue(news.get(1).getPublishedAt().contains("2026-11-24T09:30:00"));

        assertEquals("Bài báo 2: Tuyển Việt Nam hội quân", news.get(2).getTitle());
    }

    @Test
    @DisplayName("Tình huống bài trùng: Loại trùng theo URL")
    void testGetLatestNews_deduplicationByUrl() throws Exception {
        when(feedFetcher.fetch(NewsService.VNEXPRESS_FEED)).thenReturn(SAMPLE_VNEXPRESS_XML);
        when(feedFetcher.fetch(NewsService.TUOITRE_FEED)).thenReturn(SAMPLE_TUOITRE_XML);

        List<NewsItemDto> news = newsService.getLatestNews();

        long matchingCount = news.stream()
                .filter(n -> n.getUrl().contains("man-utd-chien-thang-1.html"))
                .count();
        assertEquals(1, matchingCount, "Chỉ được giữ 1 bản ghi duy nhất cho link trùng");
    }

    @Test
    @DisplayName("Bỏ bản ghi thiếu tiêu đề hoặc link")
    void testGetLatestNews_dropsInvalidItems() throws Exception {
        when(feedFetcher.fetch(NewsService.VNEXPRESS_FEED)).thenReturn(SAMPLE_VNEXPRESS_XML);
        when(feedFetcher.fetch(NewsService.TUOITRE_FEED)).thenReturn(SAMPLE_TUOITRE_XML);

        List<NewsItemDto> news = newsService.getLatestNews();

        for (NewsItemDto item : news) {
            assertFalse(item.getTitle().isBlank());
            assertFalse(item.getUrl().isBlank());
        }
    }

    @Test
    @DisplayName("Tình huống một nguồn lỗi: VnExpress lỗi, Tuổi Trẻ thành công -> Vẫn trả tin từ Tuổi Trẻ")
    void testGetLatestNews_oneSourceFails_stillReturnsOtherSource() throws Exception {
        when(feedFetcher.fetch(NewsService.VNEXPRESS_FEED)).thenThrow(new RuntimeException("VnExpress 500 Internal Error"));
        when(feedFetcher.fetch(NewsService.TUOITRE_FEED)).thenReturn(SAMPLE_TUOITRE_XML);

        List<NewsItemDto> news = newsService.getLatestNews();

        assertNotNull(news);
        assertFalse(news.isEmpty());
        assertTrue(news.stream().allMatch(n -> "Tuổi Trẻ".equals(n.getSource())));
        assertEquals(2, news.size());
    }

    @Test
    @DisplayName("Tình huống cả hai nguồn lỗi khi đã có cache: Trả về bản cache gần nhất")
    void testGetLatestNews_bothSourcesFail_returnsFallbackCache() throws Exception {
        // Lần 1: Thành công tạo cache
        when(feedFetcher.fetch(NewsService.VNEXPRESS_FEED)).thenReturn(SAMPLE_VNEXPRESS_XML);
        when(feedFetcher.fetch(NewsService.TUOITRE_FEED)).thenReturn(SAMPLE_TUOITRE_XML);
        List<NewsItemDto> initialNews = newsService.getLatestNews();
        assertFalse(initialNews.isEmpty());
        assertEquals(3, initialNews.size());

        // Cho thời gian trôi qua 20 phút (quá 15 phút TTL của cache)
        clock.advance(Duration.ofMinutes(20));

        // Cả 2 nguồn mạng đều bị lỗi
        doThrow(new RuntimeException("VnExpress Timeout")).when(feedFetcher).fetch(NewsService.VNEXPRESS_FEED);
        doThrow(new RuntimeException("Tuổi Trẻ Timeout")).when(feedFetcher).fetch(NewsService.TUOITRE_FEED);

        // Lần gọi này cố gắng làm mới nhưng cả 2 lỗi -> fallback về bản cache trước đó
        List<NewsItemDto> fallbackNews = newsService.getLatestNews();
        assertNotNull(fallbackNews);
        assertEquals(3, fallbackNews.size(), "Phải trả về bản cache gần nhất khi cả 2 nguồn hỏng");
        assertEquals("Bài báo 1: Man Utd chiến thắng kịch tính", fallbackNews.get(0).getTitle());
    }

    @Test
    @DisplayName("Tình huống cả hai nguồn lỗi khi chưa có cache: Ném ngoại lệ rõ ràng")
    void testGetLatestNews_bothSourcesFail_noCache_throwsException() throws Exception {
        when(feedFetcher.fetch(anyString())).thenThrow(new RuntimeException("Connection refused"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> newsService.getLatestNews());
        assertTrue(ex.getMessage().contains("Không thể tải tin tức thể thao lúc này"));
    }

    @Test
    @DisplayName("Bộ nhớ cache 15 phút: Lần gọi thứ 2 trong vòng 15 phút không gọi lại feedFetcher")
    void testGetLatestNews_caching15Minutes() throws Exception {
        when(feedFetcher.fetch(NewsService.VNEXPRESS_FEED)).thenReturn(SAMPLE_VNEXPRESS_XML);
        when(feedFetcher.fetch(NewsService.TUOITRE_FEED)).thenReturn(SAMPLE_TUOITRE_XML);

        // Lần gọi 1
        newsService.getLatestNews();
        verify(feedFetcher, times(1)).fetch(NewsService.VNEXPRESS_FEED);
        verify(feedFetcher, times(1)).fetch(NewsService.TUOITRE_FEED);

        // Lần gọi 2 (ngay sau đó, dưới 15 phút)
        newsService.getLatestNews();
        // Số lần gọi fetch vẫn chỉ là 1
        verify(feedFetcher, times(1)).fetch(NewsService.VNEXPRESS_FEED);
        verify(feedFetcher, times(1)).fetch(NewsService.TUOITRE_FEED);
    }
}
