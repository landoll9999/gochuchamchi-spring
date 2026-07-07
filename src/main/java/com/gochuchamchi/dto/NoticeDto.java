package com.gochuchamchi.dto;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class NoticeDto {
    private Long id;
    private String title;
    private String content;
    private String author;
    private boolean pinned;
    private int views;
    private LocalDateTime createdAt;

    public String getCreatedAtStr() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}