package com.example.groupshop.membercard;

import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.membercard.dto.MemberCardListResponse;
import com.example.groupshop.membercard.dto.MemberCardResponse;
import com.example.groupshop.membercard.service.MemberCardService;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MemberCardService}.
 */
@Transactional
class MemberCardServiceTest extends ServiceTestBase {

    @Autowired
    private MemberCardService memberCardService;

    @Autowired
    private MemberRelationMapper memberRelationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private StoreMapper storeMapper;

    private Long userId;
    private Long leaderId;
    private Long storeId;

    @BeforeEach
    void setUp() {
        // Set up user (buyer)
        User user = new User();
        user.setNickname("买家");
        user.setPhone("13800014001");
        user.setStatus("normal");
        userMapper.insert(user);
        userId = user.getId();

        // Set up leader
        User leaderUser = new User();
        leaderUser.setNickname("团长");
        leaderUser.setPhone("13800014002");
        leaderUser.setStatus("normal");
        userMapper.insert(leaderUser);

        Leader leader = new Leader();
        leader.setUserId(leaderUser.getId());
        leader.setDisplayName("测试团长");
        leader.setAvatarUrl("https://example.com/leader-avatar.png");
        leader.setServiceStatus("normal");
        leader.setMemberCount(0);
        leader.setFollowerCount(0);
        leaderMapper.insert(leader);
        leaderId = leader.getId();

        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName("测试店铺");
        store.setLogoUrl("https://example.com/store-logo.png");
        store.setDefaultDeliveryType(DeliveryType.EXPRESS.getValue());
        store.setDistributionEnabled(false);
        store.setStatus("active");
        storeMapper.insert(store);
        storeId = store.getId();

        // Create member relation
        MemberRelation relation = new MemberRelation();
        relation.setUserId(userId);
        relation.setLeaderId(leaderId);
        relation.setStoreId(storeId);
        relation.setLevelName("V0");
        relation.setGrowthValue(2990);
        relation.setTotalOrderAmount(2990L);
        relation.setTotalOrders(1);
        relation.setLastOrderAt(LocalDateTime.now());
        memberRelationMapper.insert(relation);
    }

    @Test
    void listMyMemberCards_shouldReturnCards() {
        MemberCardListResponse response = memberCardService.listMyMemberCards(userId);

        assertThat(response.getItems()).isNotEmpty();
        MemberCardResponse card = response.getItems().get(0);

        assertThat(card.getId()).isPositive();
        assertThat(card.getLevelName()).isEqualTo("V0");
        assertThat(card.getGrowthValue()).isEqualTo(2990);
        assertThat(card.getTotalOrderAmount()).isEqualTo(2990L);
        assertThat(card.getTotalOrders()).isEqualTo(1);
        assertThat(card.getLastOrderAt()).isNotNull();

        // Verify nested leader info
        assertThat(card.getLeader()).isNotNull();
        assertThat(card.getLeader().getId()).isEqualTo(leaderId);
        assertThat(card.getLeader().getDisplayName()).isEqualTo("测试团长");
        assertThat(card.getLeader().getAvatarUrl()).isEqualTo("https://example.com/leader-avatar.png");

        // Verify nested store info
        assertThat(card.getStore()).isNotNull();
        assertThat(card.getStore().getId()).isEqualTo(storeId);
        assertThat(card.getStore().getName()).isEqualTo("测试店铺");
        assertThat(card.getStore().getLogoUrl()).isEqualTo("https://example.com/store-logo.png");
    }

    @Test
    void listMyMemberCards_shouldReturnEmptyWhenNoRelations() {
        Long otherUserId = 99999L;
        MemberCardListResponse response = memberCardService.listMyMemberCards(otherUserId);

        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void listMyMemberCards_shouldReturnMultipleCards() {
        // Create a second leader + store + member relation
        User leaderUser2 = new User();
        leaderUser2.setNickname("团长2");
        leaderUser2.setPhone("13800014003");
        leaderUser2.setStatus("normal");
        userMapper.insert(leaderUser2);

        Leader leader2 = new Leader();
        leader2.setUserId(leaderUser2.getId());
        leader2.setDisplayName("测试团长2");
        leader2.setServiceStatus("normal");
        leader2.setMemberCount(0);
        leader2.setFollowerCount(0);
        leaderMapper.insert(leader2);

        Store store2 = new Store();
        store2.setLeaderId(leader2.getId());
        store2.setName("测试店铺2");
        store2.setDefaultDeliveryType(DeliveryType.EXPRESS.getValue());
        store2.setDistributionEnabled(false);
        store2.setStatus("active");
        storeMapper.insert(store2);

        MemberRelation relation2 = new MemberRelation();
        relation2.setUserId(userId);
        relation2.setLeaderId(leader2.getId());
        relation2.setStoreId(store2.getId());
        relation2.setLevelName("V0");
        relation2.setGrowthValue(1000);
        relation2.setTotalOrderAmount(1000L);
        relation2.setTotalOrders(1);
        relation2.setLastOrderAt(LocalDateTime.now());
        memberRelationMapper.insert(relation2);

        MemberCardListResponse response = memberCardService.listMyMemberCards(userId);
        assertThat(response.getItems()).hasSize(2);
    }
}
