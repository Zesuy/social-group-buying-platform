package com.example.groupshop.memberlevel;

import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.memberlevel.dto.MemberLevelRuleItem;
import com.example.groupshop.memberlevel.dto.MemberLevelRulesResponse;
import com.example.groupshop.memberlevel.dto.UpdateMemberLevelRulesRequest;
import com.example.groupshop.memberlevel.service.MemberLevelRuleService;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.MemberRelation;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.MemberRelationMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MemberLevelRuleService}.
 */
@Transactional
class MemberLevelRuleServiceTest extends ServiceTestBase {

    @Autowired
    private MemberLevelRuleService memberLevelRuleService;

    @Autowired
    private MemberRelationMapper memberRelationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private StoreMapper storeMapper;

    private Long storeOwnerUserId;
    private Long storeId;
    private Long buyerUserId;

    @BeforeEach
    void setUp() {
        // Set up store owner
        User owner = new User();
        owner.setNickname("店主");
        owner.setPhone("13800040001");
        owner.setStatus("normal");
        userMapper.insert(owner);
        storeOwnerUserId = owner.getId();

        Leader leader = new Leader();
        leader.setUserId(owner.getId());
        leader.setDisplayName("测试店主");
        leader.setServiceStatus("normal");
        leader.setMemberCount(0);
        leader.setFollowerCount(0);
        leaderMapper.insert(leader);

        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName("等级测试店铺");
        store.setDefaultDeliveryType(DeliveryType.EXPRESS.getValue());
        store.setDistributionEnabled(false);
        store.setStatus("active");
        storeMapper.insert(store);
        storeId = store.getId();

        // Set up buyer
        User buyer = new User();
        buyer.setNickname("买家");
        buyer.setPhone("13800040002");
        buyer.setStatus("normal");
        userMapper.insert(buyer);
        buyerUserId = buyer.getId();

        // Create a member relation for the buyer
        MemberRelation relation = new MemberRelation();
        relation.setUserId(buyerUserId);
        relation.setLeaderId(leader.getId());
        relation.setStoreId(storeId);
        relation.setLevelName("V0");
        relation.setGrowthValue(500);
        relation.setTotalOrderAmount(500L);
        relation.setTotalOrders(1);
        relation.setLastOrderAt(LocalDateTime.now());
        memberRelationMapper.insert(relation);
    }

    @Test
    void getRules_shouldReturnEmptyWhenNoRules() {
        MemberLevelRulesResponse response = memberLevelRuleService.getRules(storeOwnerUserId);
        assertThat(response.getRules()).isEmpty();
    }

    @Test
    void updateRules_shouldReplaceRules() {
        UpdateMemberLevelRulesRequest request = new UpdateMemberLevelRulesRequest();
        request.setRules(List.of(
                MemberLevelRuleItem.builder().levelName("V0").minGrowthValue(0).build(),
                MemberLevelRuleItem.builder().levelName("V1").minGrowthValue(1000).build(),
                MemberLevelRuleItem.builder().levelName("V2").minGrowthValue(5000).build()
        ));

        MemberLevelRulesResponse response = memberLevelRuleService.updateRules(storeOwnerUserId, request);

        assertThat(response.getRules()).hasSize(3);
    }

    @Test
    void updateRules_shouldFailWhenNoBaseRule() {
        UpdateMemberLevelRulesRequest request = new UpdateMemberLevelRulesRequest();
        request.setRules(List.of(
                MemberLevelRuleItem.builder().levelName("V1").minGrowthValue(1000).build()
        ));

        assertThatThrownBy(() -> memberLevelRuleService.updateRules(storeOwnerUserId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void updateRules_shouldFailWhenThresholdsNotAscending() {
        UpdateMemberLevelRulesRequest request = new UpdateMemberLevelRulesRequest();
        request.setRules(List.of(
                MemberLevelRuleItem.builder().levelName("V0").minGrowthValue(0).build(),
                MemberLevelRuleItem.builder().levelName("V1").minGrowthValue(1000).build(),
                MemberLevelRuleItem.builder().levelName("V2").minGrowthValue(500).build() // not ascending
        ));

        assertThatThrownBy(() -> memberLevelRuleService.updateRules(storeOwnerUserId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void resolveLevelName_shouldMatchHighestThreshold() {
        // Set up rules: V0=0, V1=1000, V2=5000
        UpdateMemberLevelRulesRequest request = new UpdateMemberLevelRulesRequest();
        request.setRules(List.of(
                MemberLevelRuleItem.builder().levelName("V0").minGrowthValue(0).build(),
                MemberLevelRuleItem.builder().levelName("V1").minGrowthValue(1000).build(),
                MemberLevelRuleItem.builder().levelName("V2").minGrowthValue(5000).build()
        ));
        memberLevelRuleService.updateRules(storeOwnerUserId, request);

        // Growth 500 → V0
        assertThat(memberLevelRuleService.resolveLevelName(storeId, 500)).isEqualTo("V0");
        // Growth 1000 → V1
        assertThat(memberLevelRuleService.resolveLevelName(storeId, 1000)).isEqualTo("V1");
        // Growth 3000 → V1
        assertThat(memberLevelRuleService.resolveLevelName(storeId, 3000)).isEqualTo("V1");
        // Growth 5000 → V2
        assertThat(memberLevelRuleService.resolveLevelName(storeId, 5000)).isEqualTo("V2");
        // Growth 10000 → V2
        assertThat(memberLevelRuleService.resolveLevelName(storeId, 10000)).isEqualTo("V2");
    }

    @Test
    void resolveLevelName_shouldReturnV0WhenNoRules() {
        assertThat(memberLevelRuleService.resolveLevelName(storeId, 500)).isEqualTo("V0");
        assertThat(memberLevelRuleService.resolveLevelName(storeId, 0)).isEqualTo("V0");
        assertThat(memberLevelRuleService.resolveLevelName(storeId, 99999)).isEqualTo("V0");
    }

    @Test
    void getNextLevelInfo_shouldReturnCorrectGrowthToNext() {
        UpdateMemberLevelRulesRequest request = new UpdateMemberLevelRulesRequest();
        request.setRules(List.of(
                MemberLevelRuleItem.builder().levelName("V0").minGrowthValue(0).build(),
                MemberLevelRuleItem.builder().levelName("V1").minGrowthValue(1000).build(),
                MemberLevelRuleItem.builder().levelName("V2").minGrowthValue(5000).build()
        ));
        memberLevelRuleService.updateRules(storeOwnerUserId, request);

        // Growth 500 → next is V1 at 1000, need 500 more
        MemberLevelRuleService.NextLevelInfo next = memberLevelRuleService.getNextLevelInfo(storeId, 500);
        assertThat(next).isNotNull();
        assertThat(next.getNextLevelName()).isEqualTo("V1");
        assertThat(next.getNextLevelGrowthValue()).isEqualTo(1000);
        assertThat(next.getGrowthToNextLevel()).isEqualTo(500);

        // Growth 5000 → already at max level, no next
        MemberLevelRuleService.NextLevelInfo nextMax = memberLevelRuleService.getNextLevelInfo(storeId, 5000);
        assertThat(nextMax).isNull();
    }

    @Test
    void getNextLevelInfo_shouldReturnNullWhenNoRules() {
        MemberLevelRuleService.NextLevelInfo next = memberLevelRuleService.getNextLevelInfo(storeId, 500);
        assertThat(next).isNull();
    }

    @Test
    void updateRules_shouldRecalculateMemberLevels() {
        // Buyer has growth 500 at V0
        // Set higher threshold rules
        UpdateMemberLevelRulesRequest request = new UpdateMemberLevelRulesRequest();
        request.setRules(List.of(
                MemberLevelRuleItem.builder().levelName("V0").minGrowthValue(0).build(),
                MemberLevelRuleItem.builder().levelName("V1").minGrowthValue(1000).build()
        ));
        memberLevelRuleService.updateRules(storeOwnerUserId, request);

        // Buyer with growth 500 stays V0 (not reached V1 threshold)
        MemberRelation relation = memberRelationMapper.selectById(
                memberRelationMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MemberRelation>()
                                .eq(MemberRelation::getUserId, buyerUserId)
                                .eq(MemberRelation::getStoreId, storeId)
                ).get(0).getId());
        assertThat(relation.getLevelName()).isEqualTo("V0");
    }

    @Test
    void recalculateMemberLevel_shouldUpdateSingleRelation() {
        UpdateMemberLevelRulesRequest request = new UpdateMemberLevelRulesRequest();
        request.setRules(List.of(
                MemberLevelRuleItem.builder().levelName("V0").minGrowthValue(0).build(),
                MemberLevelRuleItem.builder().levelName("V1").minGrowthValue(1000).build()
        ));
        memberLevelRuleService.updateRules(storeOwnerUserId, request);

        // Set growth to 2000 via recalculate
        MemberRelation relation = memberRelationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MemberRelation>()
                        .eq(MemberRelation::getUserId, buyerUserId)
                        .eq(MemberRelation::getStoreId, storeId));
        memberLevelRuleService.recalculateMemberLevel(storeId, relation.getId(), 2000);

        MemberRelation updated = memberRelationMapper.selectById(relation.getId());
        assertThat(updated.getLevelName()).isEqualTo("V1");
    }
}
