package com.gochuchamchi.mapper;

import com.gochuchamchi.dto.NoticeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface NoticeMapper {
    List<NoticeDto> findAll(@Param("offset") int offset, @Param("limit") int limit,
                            @Param("keyword") String keyword, @Param("type") String type,
                            @Param("period") String period);
    NoticeDto findById(Long id);
    int countAll(@Param("keyword") String keyword, @Param("type") String type, @Param("period") String period);
    void incrementViews(Long id);

    // 관리자용
    List<NoticeDto> findAllForAdmin();
    void insert(NoticeDto notice);
    void update(NoticeDto notice);
    void delete(Long id);
}
