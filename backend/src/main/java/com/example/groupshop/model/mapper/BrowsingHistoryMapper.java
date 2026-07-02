package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.BrowsingHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for {@link BrowsingHistory}.
 */
@Mapper
public interface BrowsingHistoryMapper extends BaseMapper<BrowsingHistory> {
}
