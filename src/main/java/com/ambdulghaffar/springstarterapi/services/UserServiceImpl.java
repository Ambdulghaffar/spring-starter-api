package com.ambdulghaffar.springstarterapi.services;

import com.ambdulghaffar.springstarterapi.config.SecurityUtils;
import com.ambdulghaffar.springstarterapi.dto.PaginationDto;
import com.ambdulghaffar.springstarterapi.dto.UserDto;
import com.ambdulghaffar.springstarterapi.dto.shared.PageResponse;
import com.ambdulghaffar.springstarterapi.entity.User;
import com.ambdulghaffar.springstarterapi.entity.enums.MediaType;
import com.ambdulghaffar.springstarterapi.entity.enums.UserEnum;
import com.ambdulghaffar.springstarterapi.mapper.UserMapper;
import com.ambdulghaffar.springstarterapi.repository.*;
import com.ambdulghaffar.springstarterapi.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final MediaService mediaService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailTokenRepository emailTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDto.Response> getAll(
            UserDto.Filter filter,
            PaginationDto.Pagination pagination
    ) {
        log.debug(
                "Fetching users - page: {}, size: {}, role: {}, status: {}",
                pagination.page(),
                pagination.size(),
                filter.role(),
                filter.status()
        );

        Page<UserDto.Response> users = userRepository
                .findAll(
                        UserSpecification.from(filter),
                        pagination.toPageable()
                )
                .map(userMapper::toDto);

        log.debug(
                "Found {} users (total: {})",
                users.getNumberOfElements(),
                users.getTotalElements()
        );

        return PageResponse.from(users);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto.Response getById(Long id) {
        log.debug("Fetching user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Aucun utilisateur trouvé avec l'ID: " + id
                    );
                });

        return userMapper.toDto(user);
    }


    @Override
    @Transactional
    public UserDto.Response createUser(UserDto.Request request) {
        log.debug("Creating user with email: {}", request.email());

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        applyImages(user, request.imageIds());

        User savedUser = userRepository.save(user);

        log.info("User created successfully with id: {}", savedUser.getId());

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto.Response updateUser(Long id, UserDto.Request request) {
        log.debug("Updating user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Update failed - user not found with id: {}", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Aucun utilisateur trouvé avec l'ID: " + id
                    );
                });

        if (user.getDeletedAt() != null || user.getStatus() == UserEnum.UserStatus.DELETED) {
            log.warn("Update failed - user already deleted with id: {}", id);
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Aucun utilisateur trouvé avec l'ID: " + id
            );
        }

        userMapper.updateEntity(request, user);

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        applyImages(user, request.imageIds());

        User updatedUser = userRepository.save(user);

        log.info("User updated successfully with id: {}", id);

        return userMapper.toDto(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto.Response getMe() {
        return userMapper.toDto(securityUtils.getCurrentUser());
    }

    @Override
    @Transactional
    public UserDto.Response updateMe(UserDto.UpdateProfile request) {
        User user = securityUtils.getCurrentUser();
        userMapper.updateEntity(request, user);
        applyImages(user, request.imageIds());
        return userMapper.toDto(userRepository.save(user));
    }

    private void applyImages(User user, List<Long> imageIds) {
        if (imageIds == null) {
            return;
        }
        if (imageIds.size() > 10
                || imageIds.contains(null)
                || new HashSet<>(imageIds).size() != imageIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La galerie doit contenir au maximum 10 images distinctes."
            );
        }
        List<com.ambdulghaffar.springstarterapi.entity.Media> images = imageIds.stream()
                .map(id -> mediaService.requiredOwnerOrAdmin(id, user.getId(), MediaType.IMAGE))
                .toList();
        user.getImages().clear();
        user.getImages().addAll(images);
    }

    @Override
    @Transactional
    public void softDeleteUser(Long id) {
        log.debug("Deleting user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Delete failed - user not found with id: {}", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Aucun utilisateur trouvé avec l'ID: " + id
                    );
                });

        if (user.getDeletedAt() != null
                || user.getStatus() == UserEnum.UserStatus.DELETED) {
            log.warn("Delete failed - user already deleted with id: {}", id);
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Aucun utilisateur trouvé avec l'ID: " + id
            );
        }

        userRepository.delete(user);

        log.info("User deleted successfully with id: {}", id);
    }

    @Override
    @Transactional
    public void hardDeleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun utilisateur trouvé avec l'ID : " + id
                ));

        if (user.getDeletedAt() == null || user.getStatus() != UserEnum.UserStatus.DELETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "L'utilisateur doit être supprimé logiquement avant d'être purgé."
            );
        }

        emailTokenRepository.deleteByUser_Id(id);
        refreshTokenRepository.deleteByUserId(id.toString());
        mediaService.deleteAllOfUser(id);

        userRepository.hardDelete(id);
    }
}