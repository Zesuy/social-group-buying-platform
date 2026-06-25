package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for {@link User}.
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
