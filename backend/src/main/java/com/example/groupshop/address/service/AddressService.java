package com.example.groupshop.address.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.groupshop.address.dto.AddressResponse;
import com.example.groupshop.address.dto.CreateAddressRequest;
import com.example.groupshop.address.dto.UpdateAddressRequest;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.model.entity.Address;
import com.example.groupshop.model.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for address management.
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressMapper addressMapper;

    /**
     * List addresses for the current user, default address first.
     */
    public List<AddressResponse> listAddresses(Long userId) {
        List<Address> addresses = addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault)
                        .orderByDesc(Address::getUpdatedAt));
        return addresses.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Create a new address.
     * The first address is automatically set as default.
     */
    @Transactional
    public AddressResponse createAddress(Long userId, CreateAddressRequest request) {
        long count = addressMapper.selectCount(
                new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));

        boolean isDefault = request.getIsDefault() != null && request.getIsDefault();

        // If this is the first address, force it as default
        if (count == 0) {
            isDefault = true;
        }

        // If setting as default, unset others
        if (isDefault) {
            unsetOtherDefaults(userId, null);
        }

        Address address = new Address();
        address.setUserId(userId);
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetail(request.getDetail());
        address.setIsDefault(isDefault);
        addressMapper.insert(address);

        return toResponse(address);
    }

    /**
     * Partial-update an address. Only the current user can update their own address.
     */
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, UpdateAddressRequest request) {
        Address address = findAddressForUser(addressId, userId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        if (request.getReceiverName() != null) {
            address.setReceiverName(request.getReceiverName());
        }
        if (request.getReceiverPhone() != null) {
            address.setReceiverPhone(request.getReceiverPhone());
        }
        if (request.getProvince() != null) {
            address.setProvince(request.getProvince());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getDistrict() != null) {
            address.setDistrict(request.getDistrict());
        }
        if (request.getDetail() != null) {
            address.setDetail(request.getDetail());
        }
        if (request.getIsDefault() != null && request.getIsDefault()) {
            unsetOtherDefaults(userId, addressId);
            address.setIsDefault(true);
        } else if (request.getIsDefault() != null) {
            // Un-setting default: if this is the only address, keep it as default
            long count = addressMapper.selectCount(
                    new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));
            if (count <= 1 && wasDefault) {
                address.setIsDefault(true); // can't unset the last default
            } else {
                address.setIsDefault(false);
                // If the address was the default, promote the newest remaining address
                if (wasDefault) {
                    promoteNewestRemaining(userId, addressId);
                }
            }
        }

        addressMapper.updateById(address);
        return toResponse(address);
    }

    /**
     * Delete an address. Only the current user can delete their own address.
     * If the deleted address was default, the newest remaining address becomes default.
     */
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findAddressForUser(addressId, userId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressMapper.deleteById(addressId);

        // If the deleted address was default, promote the newest remaining address
        if (wasDefault) {
            promoteNewestRemaining(userId, addressId);
        }
    }

    /**
     * Promote the newest remaining address (excluding excludeAddressId) to default,
     * skipping if the address is already default.
     */
    private void promoteNewestRemaining(Long userId, Long excludeAddressId) {
        List<Address> remaining = addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .ne(Address::getId, excludeAddressId)
                        .orderByDesc(Address::getCreatedAt));
        if (!remaining.isEmpty() && !Boolean.TRUE.equals(remaining.get(0).getIsDefault())) {
            Address newDefault = remaining.get(0);
            newDefault.setIsDefault(true);
            addressMapper.updateById(newDefault);
        }
    }

    /**
     * Get a single address response by ID, verifying ownership.
     */
    public AddressResponse getAddress(Long addressId, Long userId) {
        Address address = findAddressForUser(addressId, userId);
        return toResponse(address);
    }

    /**
     * Internal: find an address and verify it belongs to the given user.
     */
    public Address findAddressForUser(Long addressId, Long userId) {
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ADDRESS_FORBIDDEN);
        }
        return address;
    }

    private void unsetOtherDefaults(Long userId, Long excludeAddressId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, true);
        if (excludeAddressId != null) {
            wrapper.ne(Address::getId, excludeAddressId);
        }
        List<Address> defaults = addressMapper.selectList(wrapper);
        for (Address addr : defaults) {
            addr.setIsDefault(false);
            addressMapper.updateById(addr);
        }
    }

    public AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .province(address.getProvince())
                .city(address.getCity())
                .district(address.getDistrict())
                .detail(address.getDetail())
                .fullAddress(address.getProvince() + address.getCity()
                        + address.getDistrict() + address.getDetail())
                .isDefault(address.getIsDefault())
                .build();
    }
}
