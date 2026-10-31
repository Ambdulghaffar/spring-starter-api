package com.ambdulghaffar.springstarterapi.services;

import com.ambdulghaffar.springstarterapi.dto.PaginationDto;
import com.ambdulghaffar.springstarterapi.dto.UserDto;
import com.ambdulghaffar.springstarterapi.dto.shared.PageResponse;

public interface UserService {
    PageResponse<UserDto.Response> getAll(
            UserDto.Filter filter,
            PaginationDto.Pagination pagination
    );
    UserDto.Response getById(Long id);

    UserDto.Response createUser(UserDto.Request request);

    UserDto.Response updateUser(
            Long id,
            UserDto.Request request
    );
    UserDto.Response getMe();
    UserDto.Response updateMe(UserDto.UpdateProfile request);
    void softDeleteUser(Long id);

    void hardDeleteUser(Long id);
}
