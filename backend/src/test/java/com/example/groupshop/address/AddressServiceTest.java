package com.example.groupshop.address;

import com.example.groupshop.address.dto.AddressResponse;
import com.example.groupshop.address.dto.CreateAddressRequest;
import com.example.groupshop.address.dto.UpdateAddressRequest;
import com.example.groupshop.address.service.AddressService;
import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.model.entity.Address;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.AddressMapper;
import com.example.groupshop.model.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AddressService}.
 */
@Transactional
class AddressServiceTest extends ServiceTestBase {

    @Autowired
    private AddressService addressService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AddressMapper addressMapper;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setNickname("测试用户");
        user.setPhone("13800009901");
        user.setStatus("normal");
        userMapper.insert(user);
        userId = user.getId();
    }

    @Test
    void createAddress_shouldSetFirstAsDefault() {
        CreateAddressRequest request = new CreateAddressRequest();
        request.setReceiverName("张三");
        request.setReceiverPhone("13800000001");
        request.setProvince("浙江省");
        request.setCity("杭州市");
        request.setDistrict("西湖区");
        request.setDetail("某某路 1 号");

        AddressResponse response = addressService.createAddress(userId, request);
        assertThat(response.getIsDefault()).isTrue();
        assertThat(response.getFullAddress()).isEqualTo("浙江省杭州市西湖区某某路 1 号");
    }

    @Test
    void createAddress_shouldUnsetPreviousDefault() {
        // Create first address (auto-default)
        CreateAddressRequest req1 = new CreateAddressRequest();
        req1.setReceiverName("地址1");
        req1.setReceiverPhone("13800000001");
        req1.setProvince("浙江省");
        req1.setCity("杭州市");
        req1.setDistrict("西湖区");
        req1.setDetail("路1");
        addressService.createAddress(userId, req1);

        // Create second address with isDefault=true
        CreateAddressRequest req2 = new CreateAddressRequest();
        req2.setReceiverName("地址2");
        req2.setReceiverPhone("13800000002");
        req2.setProvince("浙江省");
        req2.setCity("杭州市");
        req2.setDistrict("上城区");
        req2.setDetail("路2");
        req2.setIsDefault(true);
        AddressResponse response2 = addressService.createAddress(userId, req2);

        assertThat(response2.getIsDefault()).isTrue();

        // First address should no longer be default
        List<AddressResponse> list = addressService.listAddresses(userId);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getIsDefault()).isTrue();
        assertThat(list.get(0).getId()).isEqualTo(response2.getId());
    }

    @Test
    void updateAddress_shouldPromoteDefault() {
        CreateAddressRequest req1 = new CreateAddressRequest();
        req1.setReceiverName("地址1");
        req1.setReceiverPhone("13800000001");
        req1.setProvince("浙江省");
        req1.setCity("杭州市");
        req1.setDistrict("西湖区");
        req1.setDetail("路1");
        AddressResponse addr1 = addressService.createAddress(userId, req1);

        CreateAddressRequest req2 = new CreateAddressRequest();
        req2.setReceiverName("地址2");
        req2.setReceiverPhone("13800000002");
        req2.setProvince("浙江省");
        req2.setCity("杭州市");
        req2.setDistrict("上城区");
        req2.setDetail("路2");
        addressService.createAddress(userId, req2);

        // Update addr1 to be default
        UpdateAddressRequest updateReq = new UpdateAddressRequest();
        updateReq.setIsDefault(true);
        AddressResponse updated = addressService.updateAddress(userId, addr1.getId(), updateReq);
        assertThat(updated.getIsDefault()).isTrue();

        // Check addr2 is no longer default
        List<AddressResponse> list = addressService.listAddresses(userId);
        assertThat(list.get(0).getId()).isEqualTo(addr1.getId());
        assertThat(list.get(0).getIsDefault()).isTrue();
    }

    @Test
    void deleteAddress_shouldPromoteNewestRemaining() {
        CreateAddressRequest req1 = new CreateAddressRequest();
        req1.setReceiverName("地址1");
        req1.setReceiverPhone("13800000001");
        req1.setProvince("浙江省");
        req1.setCity("杭州市");
        req1.setDistrict("西湖区");
        req1.setDetail("路1");
        AddressResponse addr1 = addressService.createAddress(userId, req1);

        CreateAddressRequest req2 = new CreateAddressRequest();
        req2.setReceiverName("地址2");
        req2.setReceiverPhone("13800000002");
        req2.setProvince("浙江省");
        req2.setCity("杭州市");
        req2.setDistrict("上城区");
        req2.setDetail("路2");
        addressService.createAddress(userId, req2);

        // Delete the default (addr2 is the latest, should be default)
        addressService.deleteAddress(userId, addr1.getId());

        // The remaining address should now be default
        List<AddressResponse> list = addressService.listAddresses(userId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getIsDefault()).isTrue();
    }

    @Test
    void updateAddress_shouldPromoteNewestWhenUnsetDefault() {
        // Create 2 addresses (first auto-default, second also default)
        CreateAddressRequest req1 = new CreateAddressRequest();
        req1.setReceiverName("地址1");
        req1.setReceiverPhone("13800000001");
        req1.setProvince("浙江省");
        req1.setCity("杭州市");
        req1.setDistrict("西湖区");
        req1.setDetail("路1");
        AddressResponse addr1 = addressService.createAddress(userId, req1);

        CreateAddressRequest req2 = new CreateAddressRequest();
        req2.setReceiverName("地址2");
        req2.setReceiverPhone("13800000002");
        req2.setProvince("浙江省");
        req2.setCity("杭州市");
        req2.setDistrict("上城区");
        req2.setDetail("路2");
        req2.setIsDefault(true);
        AddressResponse addr2 = addressService.createAddress(userId, req2);
        assertThat(addr2.getIsDefault()).isTrue();

        // Unset default on addr2 — addr1 should become default
        UpdateAddressRequest unset = new UpdateAddressRequest();
        unset.setIsDefault(false);
        addressService.updateAddress(userId, addr2.getId(), unset);

        List<AddressResponse> list = addressService.listAddresses(userId);
        long defaultCount = list.stream().filter(AddressResponse::getIsDefault).count();
        assertThat(defaultCount).isEqualTo(1);
        // addr1 should now be the default (it's the only other address)
        AddressResponse addr1Reloaded = list.stream()
                .filter(a -> a.getId().equals(addr1.getId()))
                .findFirst().orElseThrow();
        assertThat(addr1Reloaded.getIsDefault()).isTrue();
    }

    @Test
    void getAddress_shouldThrowWhenNotFound() {
        assertThatThrownBy(() -> addressService.findAddressForUser(99999L, userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void getAddress_shouldThrowWhenForbidden() {
        CreateAddressRequest req = new CreateAddressRequest();
        req.setReceiverName("张三");
        req.setReceiverPhone("13800000001");
        req.setProvince("浙江省");
        req.setCity("杭州市");
        req.setDistrict("西湖区");
        req.setDetail("路1");
        AddressResponse addr = addressService.createAddress(userId, req);

        Long otherUserId = userId + 99999;
        assertThatThrownBy(() -> addressService.findAddressForUser(addr.getId(), otherUserId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ADDRESS_FORBIDDEN);
    }
}
