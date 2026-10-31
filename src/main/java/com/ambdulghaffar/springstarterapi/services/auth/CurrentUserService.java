package com.ambdulghaffar.springstarterapi.services.auth;

import com.ambdulghaffar.springstarterapi.dto.UserDto;
import com.ambdulghaffar.springstarterapi.dto.auth.CurrentUserDto;

public interface CurrentUserService {
    UserDto.Response getMyProfile();
    UserDto.Response updateMyProfile(UserDto.UpdateProfile request);
    void changeMyPassword(CurrentUserDto.ChangePasswordRequest request);
    void deleteMyAccount(CurrentUserDto.DeleteAccountRequest request);
}