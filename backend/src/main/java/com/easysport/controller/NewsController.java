package com.easysport.controller;

import com.easysport.dto.ApiResponse;
import com.easysport.dto.NewsItemDto;
import com.easysport.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NewsItemDto>>> getNews() {
        try {
            List<NewsItemDto> news = newsService.getLatestNews();
            return ResponseEntity.ok(ApiResponse.success("Lấy tin tức thể thao thành công", news));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Có lỗi xảy ra khi tải tin tức: " + e.getMessage()));
        }
    }
}
