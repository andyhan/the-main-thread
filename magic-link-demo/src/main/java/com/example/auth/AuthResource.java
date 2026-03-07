package com.example.auth;

import java.util.Optional;

import com.example.auth.api.LoginResponse;
import com.example.auth.api.MagicLinkRequest;
import com.example.auth.api.PasswordResetConfirmRequest;
import com.example.auth.api.RegisterRequest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    EmailService emailService;

    @Inject
    JwtService jwtService;

    @Inject
    TokenService tokenService;

    @Inject
    UserService userService;

    @Inject
    @Location("auth/reset-password.html")
    Template resetPassword;

    @POST
    @Path("/register")
    public Response register(RegisterRequest request) {
        if (request == null || request.email == null || request.password == null || request.displayName == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing registration data").build();
        }

        User user = userService.register(request.email, request.displayName, request.password);

        if (!user.emailVerified) {
            emailService.sendEmailVerification(user);
        }

        return Response.status(Response.Status.CREATED)
                .entity("Check your inbox to verify your email address")
                .build();
    }

    @POST
    @Path("/magic/request")
    public Response requestMagicLink(MagicLinkRequest request) {
        if (request != null && request.email != null) {
            User.findByEmail(request.email).ifPresent(emailService::sendMagicLink);
        }

        return Response.noContent().build();
    }

    @GET
    @Path("/magic")
    public Response consumeMagicLink(@QueryParam("token") String token) {
        Optional<User> maybeUser = tokenService.validateAndConsume(token, TokenPurpose.MAGIC_LOGIN);

        if (maybeUser.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Link is invalid, already used, or expired")
                    .build();
        }

        User user = maybeUser.get();

        if (!user.emailVerified) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Email address is not verified")
                    .build();
        }

        String jwt = jwtService.generateSessionToken(user);
        return Response.ok(new LoginResponse(jwt, user.email, user.displayName)).build();
    }

    @GET
    @Path("/reset-password")
    @Produces(MediaType.TEXT_HTML)
    public Response resetPasswordForm(@QueryParam("token") String token) {
        if (token == null || token.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing token").build();
        }
        TemplateInstance instance = resetPassword.data("token", token);
        return Response.ok(instance.render()).build();
    }

    @POST
    @Path("/password-reset/request")
    public Response requestPasswordReset(MagicLinkRequest request) {
        if (request != null && request.email != null) {
            User.findByEmail(request.email).ifPresent(emailService::sendPasswordReset);
        }
        return Response.noContent().build();
    }

    @POST
    @Path("/password-reset/confirm")
    public Response confirmPasswordReset(PasswordResetConfirmRequest request) {
        if (request == null || request.token == null || request.newPassword == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing reset data").build();
        }

        Optional<User> maybeUser = tokenService.validateAndConsume(request.token, TokenPurpose.PASSWORD_RESET);

        if (maybeUser.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Reset link is invalid, already used, or expired")
                    .build();
        }

        userService.changePassword(maybeUser.get(), request.newPassword);
        return Response.noContent().build();
    }

    @GET
    @Path("/verify-email")
    public Response verifyEmail(@QueryParam("token") String token) {
        Optional<User> maybeUser = tokenService.validateAndConsume(token, TokenPurpose.EMAIL_VERIFY);

        if (maybeUser.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Verification link is invalid, already used, or expired")
                    .build();
        }

        User user = maybeUser.get();
        userService.markEmailVerified(user.id);

        return Response.ok("Email verified").build();
    }
}