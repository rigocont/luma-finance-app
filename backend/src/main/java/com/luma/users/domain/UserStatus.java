package com.luma.users.domain;

/** Estado de la cuenta. Se guarda como texto, no como ordinal. */
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED,
    DELETED
}
