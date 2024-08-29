package core.project.chess.application.controller;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@PermitAll
@ApplicationScoped
@Path("/reception")
public class FirstReceptionController {

    @GET @Path("/home")
    public final String home() {
        return "home/home";
    }

    @GET @Path("/registration_form")
    public final String registrationForm() {
        return "login-registration/registration";
    }

    @GET @Path("/login")
    public final String loginForm() {
        return "login-registration/login";
    }

    @GET @Path("/logout")
    public final String logout() {
        return "login-registration/logout";
    }

    @GET @Path("/login?expired")
    public final String loginExpired() {
        return "login-registration/login_expired";
    }
}
