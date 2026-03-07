package com.example.auth;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EmailService {

    @Inject
    Mailer mailer;

    @Inject
    TokenService tokenService;

    @ConfigProperty(name = "app.base-url")
    String baseUrl;

    @ConfigProperty(name = "app.mail.from")
    String fromAddress;

    @CheckedTemplate
    public static class AuthTemplates {
        public static native TemplateInstance magicLink(String appName, String url, String lifetimeText);

        public static native TemplateInstance emailVerify(String url);

        public static native TemplateInstance passwordReset(String url, String lifetimeText);
    }

    public void sendMagicLink(User user) {
        String rawToken = tokenService.createToken(user, TokenPurpose.MAGIC_LOGIN);
        String url = baseUrl + "/auth/magic?token=" + rawToken;
        String html = AuthTemplates.magicLink("Magic Link Demo", url, "15 minutes").render();

        mailer.send(
                Mail.withHtml(user.email, "Your sign-in link", html)
                        .setFrom(fromAddress));
    }

    public void sendEmailVerification(User user) {
        String rawToken = tokenService.createToken(user, TokenPurpose.EMAIL_VERIFY);
        String url = baseUrl + "/auth/verify-email?token=" + rawToken;
        String html = AuthTemplates.emailVerify(url).render();

        mailer.send(
                Mail.withHtml(user.email, "Verify your email address", html)
                        .setFrom(fromAddress));
    }

    public void sendPasswordReset(User user) {
        String rawToken = tokenService.createToken(user, TokenPurpose.PASSWORD_RESET);
        String url = baseUrl + "/auth/reset-password?token=" + rawToken;
        String html = AuthTemplates.passwordReset(url, "20 minutes").render();

        mailer.send(
                Mail.withHtml(user.email, "Reset your password", html)
                        .setFrom(fromAddress));
    }
}