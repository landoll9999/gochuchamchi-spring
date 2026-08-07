package com.gochuchamchi.mapper;

import com.gochuchamchi.dto.AuditLogDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper {
    void insert(AuditLogDto auditLog);
}
