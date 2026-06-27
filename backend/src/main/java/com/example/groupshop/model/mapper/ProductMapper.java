package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for {@link Product}.
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
