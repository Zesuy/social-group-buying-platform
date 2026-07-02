package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.FavoriteItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for {@link FavoriteItem}.
 */
@Mapper
public interface FavoriteItemMapper extends BaseMapper<FavoriteItem> {
}
