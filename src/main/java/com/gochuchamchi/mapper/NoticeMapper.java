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
    NoticeDto findById(@Param("id") Long id);
    int countAll(@Param("keyword") String keyword, @Param("type") String type, @Param("period") String period);
    void incrementViews(@Param("id") Long id);
}
