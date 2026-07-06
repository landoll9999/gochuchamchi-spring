package com.gochuchamchi.mapper;

import com.gochuchamchi.dto.ProductDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProductMapper {
    List<ProductDto> findAll(@Param("offset") int offset, @Param("limit") int limit,
                             @Param("sort") String sort, @Param("category") String category);
    ProductDto findById(Long id);
    int countAll(@Param("category") String category);
    void insert(ProductDto product);
    void incrementViews(Long id);
    void delete(Long id);
}
