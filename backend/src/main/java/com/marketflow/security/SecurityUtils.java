package com.marketflow.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up;
        }
        return null;
    }

    public static String getCurrentUserId() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    public static String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            if (auth.getPrincipal() instanceof UserPrincipal up) {
                return up.getEmail();
            }
            if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                return ud.getUsername();
            }
            if (auth.getPrincipal() instanceof String s && !"anonymousUser".equals(s)) {
                return s;
            }
        }
        return null;
    }

    public static boolean isCurrentUserAdmin() {
        UserPrincipal user = getCurrentUser();
        if (user != null) {
            return user.isAdmin();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
