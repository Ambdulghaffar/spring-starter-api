package com.ambdulghaffar.springstarterapi.specification;

import com.ambdulghaffar.springstarterapi.dto.UserDto;
import com.ambdulghaffar.springstarterapi.entity.User;
import com.ambdulghaffar.springstarterapi.entity.enums.UserEnum;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> from(UserDto.Filter filter) {
        if (filter == null) {
            return notDeleted();
        }

        Specification<User> specification = Specification.allOf(
                hasRole(filter.role()),
                hasStatus(filter.status()),
                hasSearch(filter.search())
        );

        // Exclude deleted users by default.
        // When DELETED is explicitly requested, show deleted users instead.
        if (filter.status() == null || filter.status() != UserEnum.UserStatus.DELETED) {
            specification = specification.and(notDeleted());
        } else {
            specification = specification.and(isDeleted());
        }

        return specification;
    }

    private static Specification<User> hasRole(UserEnum.UserRole role) {
        if (role == null) {
            return Specification.unrestricted();
        }

        return (root, query, cb) ->
                cb.equal(root.get("role"), role);
    }

    private static Specification<User> hasStatus(UserEnum.UserStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }

        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    private static Specification<User> hasSearch(String search) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }

        String pattern = "%" + search.trim().toLowerCase() + "%";

        return (root, query, cb) -> cb.or(
                cb.like(
                        cb.lower(root.get("firstName")),
                        pattern
                ),
                cb.like(
                        cb.lower(root.get("lastName")),
                        pattern
                ),
                cb.like(
                        cb.lower(root.get("email")),
                        pattern
                )
        );
    }

    /**
     * Users who have not been logically deleted.
     */
    private static Specification<User> notDeleted() {
        return (root, query, cb) -> cb.and(
                cb.isNull(root.get("deletedAt")),
                cb.notEqual(root.get("status"), UserEnum.UserStatus.DELETED)
        );
    }

    /**
     * Users who have been logically deleted.
     */
    private static Specification<User> isDeleted() {
        return (root, query, cb) -> cb.or(
                cb.isNotNull(root.get("deletedAt")),
                cb.equal(root.get("status"), UserEnum.UserStatus.DELETED)
        );
    }
}