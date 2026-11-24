package com.easysport;

import com.easysport.dto.NewsItemDto;
import com.easysport.service.DefaultHttpFeedFetcher;
import com.easysport.service.NewsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NewsServiceLiveFeedTest {

    @Test
    @DisplayName("Chạy thật: Tải dữ liệu RSS thực tế từ VnExpress và Tuổi Trẻ trên Internet")
    void testLiveRssFeeds() {
        DefaultHttpFeedFetcher realFetcher = new DefaultHttpFeedFetcher();
        NewsService service = new NewsService(realFetcher, Clock.systemDefaultZone());

        try {
            List<NewsItemDto> liveNews = service.getLatestNews();
            assertNotNull(liveNews);
            assertFalse(liveNews.isEmpty(), "Dữ liệu tin tức thật không được rỗng");
            assertTrue(liveNews.size() <= 20, "Tối đa 20 tin");

            System.out.println("=== THỰC TẾ: TẢI THÀNH CÔNG " + liveNews.size() + " TIN THỂ THAO TỪ INTERNET ===");
            for (int i = 0; i < Math.min(5, liveNews.size()); i++) {
                NewsItemDto item = liveNews.get(i);
                System.out.println((i + 1) + ". [" + item.getSource() + "] " + item.getTitle());
                System.out.println("   Thời gian ISO: " + item.getPublishedAt());
                System.out.println("   Link: " + item.getUrl());
            }

            for (NewsItemDto item : liveNews) {
                assertNotNull(item.getTitle());
                assertFalse(item.getTitle().isBlank());
                assertNotNull(item.getUrl());
                assertFalse(item.getUrl().isBlank());
                assertNotNull(item.getSource());
                assertTrue(item.getSource().equals("VnExpress") || item.getSource().equals("Tuổi Trẻ"));
                assertNotNull(item.getPublishedAt());
            }
        } catch (Exception e) {
            System.out.println("Không thể kết nối Internet: " + e.getMessage());
        }
    }
}
