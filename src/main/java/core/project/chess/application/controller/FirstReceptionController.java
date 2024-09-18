package core.project.chess.application.controller;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@PermitAll
@Path("/reception")
public class FirstReceptionController {

    private final Template home;

    private final Template login;

    private final Template registration;

    FirstReceptionController(Template home, Template registration, Template login) {
        this.home = home;
        this.registration = registration;
        this.login = login;
    }

    @GET @Path("/home")
    public final TemplateInstance home() {
        return home.data("home");
    }

    @GET @Path("/login")
    public final TemplateInstance loginForm() {
        return login.data("login");
    }

    @GET @Path("/registration")
    public final TemplateInstance registrationForm() {
        return registration.data("registration_form");
    }
}
