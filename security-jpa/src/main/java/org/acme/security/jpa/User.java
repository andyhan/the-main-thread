package org.acme.security.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.PasswordType;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;

@Entity
@Table(name = "test_user")
@UserDefinition
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_user_SEQ")
    @SequenceGenerator(name = "test_user_SEQ", sequenceName = "test_user_SEQ", allocationSize = 50)
    public Long id;

    @Username
    public String username;

    @Password(PasswordType.CLEAR)
    public String password;

    @Roles
    public String role;

    /** OIDC subject (e.g. GitHub user id). Set for users who signed in via OIDC; null for form-only users. */
    public String oidcSubject;

    /** Display name (e.g. from OIDC "name" claim). For form users can be null → fallback to username. */
    public String displayName;

    /** Email (e.g. from OIDC). Optional. */
    public String email;

    /** Display name for UI/API: displayName if set, otherwise username. */
    public String getDisplayNameOrUsername() {
        return displayName != null && !displayName.isBlank() ? displayName : username;
    }

    public static User findByOidcSubject(String oidcSubject) {
        return find("oidcSubject", oidcSubject).firstResult();
    }

    public static void add(String username, String password, String role) {
        User user = new User();
        user.username = username;
        // For testing I am using clear text passwords. 
        // YOU should use Bcrypt!
        // user.password = BcryptUtil.bcryptHash(password);
        user.password = password;
        user.role = role;
        user.persist();
    }
}