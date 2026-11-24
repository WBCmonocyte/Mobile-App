package com.easysport.dto;

import java.util.Objects;

public class NewsItemDto {
    private String title;
    private String source;
    private String publishedAt;
    private String url;

    public NewsItemDto() {}

    public NewsItemDto(String title, String source, String publishedAt, String url) {
        this.title = title;
        this.source = source;
        this.publishedAt = publishedAt;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewsItemDto that = (NewsItemDto) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
