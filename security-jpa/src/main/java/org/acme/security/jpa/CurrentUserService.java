package org.acme.security.jpa;

import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Resolves the current request's user to the application's {@link User} entity.
 * This is the standard pattern: one app-user record per person, synced from the IdP on first use.
 * <p>
 * Form users: looked up by username (from {@link User} as used by Quarkus Security JPA).
 * OIDC users (e.g. GitHub): we read UserInfo from the identity; find or create a {@link User}
 * by OIDC subject and cache it for the request. Name and roles then come from the same place.
 */
@RequestScoped
public class CurrentUserService {

    @Inject
    SecurityIdentity identity;

    private User currentUser;

    /**
     * The app user for this request: from DB by username (form) or by OIDC subject with
     * find-or-create on first OIDC login. Use {@link User#getDisplayNameOrUsername()} for display.
     */
    @Transactional
    public User getCurrentUser() {
        if (currentUser != null) {
            return currentUser;
        }
        if (identity.isAnonymous()) {
            return null;
        }
        UserInfo userInfo = identity.getAttribute("userinfo");
        if (userInfo != null) {
            currentUser = findOrCreateOidcUser(userInfo);
        } else {
            currentUser = User.find("username", identity.getPrincipal().getName()).firstResult();
        }
        return currentUser;
    }

    private User findOrCreateOidcUser(UserInfo userInfo) {
        String sub = getOidcSub(userInfo);
        if (sub == null) {
            return null;
        }
        User user = User.findByOidcSubject(sub);
        if (user != null) {
            return user;
        }
        user = new User();
        user.oidcSubject = sub;
        user.username = "oidc:" + sub;
        user.password = "";
        user.role = "user";
        user.displayName = getString(userInfo, "name", "login");
        user.email = getString(userInfo, "email", null);
        user.persist();
        return user;
    }

    private static String getOidcSub(UserInfo userInfo) {
        try {
            String sub = userInfo.getString("sub");
            if (sub != null && !sub.isBlank()) {
                return sub;
            }
        } catch (Exception ignored) {
        }
        try {
            Object id = userInfo.get("id");
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getString(UserInfo userInfo, String key, String fallback) {
        try {
            String s = userInfo.getString(key);
            return (s != null && !s.isBlank()) ? s : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
