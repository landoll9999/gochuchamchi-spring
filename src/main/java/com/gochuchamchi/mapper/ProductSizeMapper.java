package com.gochuchamchi.mapper;
import com.gochuchamchi.dto.ProductSizeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProductSizeMapper {
    void insertSizes(@Param("sizes") List<ProductSizeDto> sizes);
    List<ProductSizeDto> findByProductId(Long productId);
}