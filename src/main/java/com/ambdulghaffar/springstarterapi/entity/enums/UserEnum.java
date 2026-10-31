package com.ambdulghaffar.springstarterapi.entity.enums;

public class UserEnum {
    public enum UserRole {
        USER,
        ADMIN
    }

    public enum UserStatus {
        PENDING_VERIFICATION,
        ACTIVE,
        BLOCKED,
        DELETED
    }

    public enum EmailTokenType {
        EMAIL_CONFIRMATION,
        PASSWORD_RESET
    }
}
