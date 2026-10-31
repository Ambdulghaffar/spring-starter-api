package com.ambdulghaffar.springstarterapi.services;

import com.ambdulghaffar.springstarterapi.dto.UserDto;
import com.ambdulghaffar.springstarterapi.entity.User;
import com.ambdulghaffar.springstarterapi.mapper.UserMapper;
import com.ambdulghaffar.springstarterapi.repository.EmailTokenRepository;
import com.ambdulghaffar.springstarterapi.repository.RefreshTokenRepository;
import com.ambdulghaffar.springstarterapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private MediaService mediaService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private EmailTokenRepository emailTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getByIdReturnsMappedUser() {
        User user = User.builder().id(7L).email("user@example.com").build();
        UserDto.Response response = new UserDto.Response(
                7L, "user@example.com", null, null, null, null,
                null, null, null, null
        );

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(response);

        assertThat(userService.getById(7L)).isSameAs(response);
    }

    @Test
    void getByIdThrowsNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }
}
