package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for {@link ProductCategory}.
 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
}
