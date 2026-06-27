package com.example.groupshop.common.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Helper to resolve the current user's leader and store identity.
 *
 * <p>All {@code /my/store/**} endpoints use this to avoid repeating
 * the user -> leader -> store lookup in every service.
 *
 * <p>Throws {@link ErrorCode#LEADER_REQUIRED} when the user is not
 * a leader or has no store.
 */
@Component
@RequiredArgsConstructor
public class CurrentStoreHelper {

    private final LeaderMapper leaderMapper;
    private final StoreMapper storeMapper;

    /**
     * Resolve the current user's leader and store.
     *
     * @throws BusinessException with LEADER_REQUIRED if the user is not a leader or has no store
     */
    public LeaderAndStore getLeaderAndStore(Long userId) {
        Leader leader = leaderMapper.selectOne(
                new LambdaQueryWrapper<Leader>()
                        .eq(Leader::getUserId, userId));
        if (leader == null) {
            throw new BusinessException(ErrorCode.LEADER_REQUIRED);
        }
        Store store = storeMapper.selectOne(
                new LambdaQueryWrapper<Store>()
                        .eq(Store::getLeaderId, leader.getId()));
        if (store == null) {
            throw new BusinessException(ErrorCode.LEADER_REQUIRED);
        }
        return new LeaderAndStore(leader, store);
    }

    @Getter
    @AllArgsConstructor
    public static class LeaderAndStore {
        private final Leader leader;
        private final Store store;
    }
}
