package com.secure.text.resource;

import com.secure.text.dto.UserDto;
import com.secure.text.util.TextSanitizer;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @POST
    public Response createUser(@Valid UserDto user) {

        if (TextSanitizer.isSuspiciousMixedScript(user.username)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Username looks suspicious (mixed scripts detected)\"}")
                    .build();
        }

        user.username = TextSanitizer.sanitize(user.username);
        user.bio = TextSanitizer.sanitize(user.bio);

        return Response.ok(user).build();
    }
}