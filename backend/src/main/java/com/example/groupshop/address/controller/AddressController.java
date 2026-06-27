package com.example.groupshop.address.controller;

import com.example.groupshop.address.dto.AddressResponse;
import com.example.groupshop.address.dto.CreateAddressRequest;
import com.example.groupshop.address.dto.UpdateAddressRequest;
import com.example.groupshop.address.service.AddressService;
import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Address management controller.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/my/addresses")
    public ApiResponse<List<AddressResponse>> listAddresses(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.success(addressService.listAddresses(userId));
    }

    @PostMapping("/my/addresses")
    public ApiResponse<AddressResponse> createAddress(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody CreateAddressRequest request) {
        return ApiResponse.success(addressService.createAddress(userId, request));
    }

    @PatchMapping("/my/addresses/{addressId}")
    public ApiResponse<AddressResponse> updateAddress(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressRequest request) {
        return ApiResponse.success(addressService.updateAddress(userId, addressId, request));
    }

    @DeleteMapping("/my/addresses/{addressId}")
    public ApiResponse<Void> deleteAddress(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
        return ApiResponse.success(null);
    }
}
