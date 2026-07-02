package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.IdempotencyKey;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for {@link IdempotencyKey}.
 */
@Mapper
public interface IdempotencyKeyMapper extends BaseMapper<IdempotencyKey> {
}
