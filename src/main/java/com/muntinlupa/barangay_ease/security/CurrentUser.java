package com.muntinlupa.barangay_ease.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() { }
    public static AppUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser user))
            throw new IllegalStateException("Authenticated user is unavailable.");
        return user;
    }
    public static boolean isAdmin() { return "ADMIN".equalsIgnoreCase(get().role()); }
}
