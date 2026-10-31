package com.ambdulghaffar.springstarterapi.services.auth;

import com.ambdulghaffar.springstarterapi.config.SecurityUtils;
import com.ambdulghaffar.springstarterapi.dto.UserDto;
import com.ambdulghaffar.springstarterapi.dto.auth.CurrentUserDto;
import com.ambdulghaffar.springstarterapi.entity.User;
import com.ambdulghaffar.springstarterapi.entity.enums.UserEnum;
import com.ambdulghaffar.springstarterapi.repository.UserRepository;
import com.ambdulghaffar.springstarterapi.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

    private static final String MESSAGE_DELETE_ACCOUNT = "supprimer mon compte définitivement";

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public UserDto.Response getMyProfile() {
        return userService.getMe();
    }

    @Override
    @Transactional
    public UserDto.Response updateMyProfile(UserDto.UpdateProfile request) {
        return userService.updateMe(request);
    }

    @Override
    @Transactional
    public void changeMyPassword(CurrentUserDto.ChangePasswordRequest request) {
        User user = securityUtils.getCurrentUser();
        log.debug("Password change attempt for user id: {}", user.getId());

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            log.warn("Password change failed - incorrect current password for user id: {}", user.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe actuel est incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user id: {}", user.getId());
    }

    @Override
    @Transactional
    public void deleteMyAccount(CurrentUserDto.DeleteAccountRequest request) {
        User user = securityUtils.getCurrentUser();
        log.debug("Account deletion attempt for user id: {}", user.getId());

        if (request.message() == null || !request.message().trim().equalsIgnoreCase(MESSAGE_DELETE_ACCOUNT)) {
            log.warn("Account deletion failed - incorrect confirmation message for user id: {}", user.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous devez taper: " + MESSAGE_DELETE_ACCOUNT);
        }

        if (user.getDeletedAt() != null
                || user.getStatus() == UserEnum.UserStatus.DELETED) {
            log.warn("Delete failed - user already deleted with id: {}", user.getId());
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "L'utilisateur " + user.getId() + " n'existe pas."
            );
        }

        userRepository.delete(user);
        log.info("Account deleted successfully for user id: {}", user.getId());
    }
}