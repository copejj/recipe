package com.braindribbler.recipe.security.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Container file for all custom Braindribbler security authentication
 * exceptions.
 */
public class AuthenticationExceptions {

    // Prevent instantiation of the container class
    private AuthenticationExceptions() {
    }

    public static class AccountInactiveException extends AuthenticationException {
        public AccountInactiveException(String msg) {
            super(msg);
        }
    }

    public static class AccountUnverifiedException extends AuthenticationException {
        public AccountUnverifiedException(String msg) {
            super(msg);
        }
    }

    public static class IpBruteForceLockedException extends AuthenticationException {
        public IpBruteForceLockedException(String msg) {
            super(msg);
        }
    }
}
