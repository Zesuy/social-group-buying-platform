package com.example.groupshop.address.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for partial-updating an address.
 */
@Data
public class UpdateAddressRequest {

    @Size(max = 64)
    private String receiverName;

    @Size(max = 20)
    private String receiverPhone;

    @Size(max = 32)
    private String province;

    @Size(max = 32)
    private String city;

    @Size(max = 32)
    private String district;

    @Size(max = 256)
    private String detail;

    private Boolean isDefault;
}
