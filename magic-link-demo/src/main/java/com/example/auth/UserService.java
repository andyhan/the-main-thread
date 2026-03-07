package com.example.auth;

import java.util.Optional;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserService {

    @Transactional
    public User register(String email, String displayName, String password) {
        Optional<User> existing = User.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }

        User user = new User();
        user.email = email.trim().toLowerCase();
        user.displayName = displayName;
        user.passwordHash = BcryptUtil.bcryptHash(password);
        user.emailVerified = false;
        user.persist();
        return user;
    }

    @Transactional
    public void markEmailVerified(long userId) {
        User user = User.findById(userId);
        if (user != null) {
            user.emailVerified = true;
        }
    }

    @Transactional
    public void changePassword(User user, String newPassword) {
        if (newPassword == null || newPassword.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters long");
        }
        user.passwordHash = BcryptUtil.bcryptHash(newPassword);
    }
}