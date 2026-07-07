package com.gochuchamchi.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NoticeDto {
    private Long id;
    private String title;
    private String content;
    private String author;
    private boolean pinned;       // isPinned → pinned로 변경 (Lombok getter 충돌 방지)
    private int views;
    private LocalDateTime createdAt;
}
