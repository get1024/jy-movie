package com.movie.api.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;

/** Keeps profile edits independent from password changes, including old client payloads. */
public final class PasswordSupport {
    private PasswordSupport() {
    }

    /** Null means omit the password column entirely, never rewrite a previously read hash. */
    public static String forUpdate(String submitted, String existing, BCryptPasswordEncoder encoder) {
        if (!StringUtils.hasText(submitted) || submitted.equals(existing)) {
            return null;
        }
        return encoder.encode(submitted);
    }

    public static String forCreate(String submitted, BCryptPasswordEncoder encoder) {
        if (!StringUtils.hasText(submitted)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return encoder.encode(submitted);
    }
}
