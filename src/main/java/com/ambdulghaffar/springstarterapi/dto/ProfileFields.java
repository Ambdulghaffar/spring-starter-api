package com.ambdulghaffar.springstarterapi.dto;

import com.ambdulghaffar.springstarterapi.dto.validation.ValidPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public interface ProfileFields {
    @NotBlank @Size(max = 200) String firstName();
    @NotBlank @Size(max = 200) String lastName();
    @NotBlank @Email @Size(max = 255) String email();
    @ValidPhone
    String phoneNumber();
}