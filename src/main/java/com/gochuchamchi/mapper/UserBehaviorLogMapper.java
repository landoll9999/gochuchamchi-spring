package com.gochuchamchi.mapper;

import com.gochuchamchi.dto.UserBehaviorLogDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserBehaviorLogMapper {
    void insert(UserBehaviorLogDto behaviorLog);
}
