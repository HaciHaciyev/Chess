package core.project.chess.application.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FirstReceptionController {

    @GetMapping("/home")
    final String home() {
        return "home/home";
    }

    @GetMapping("/registration_form")
    final String registrationForm() {
        return "login-registration/registration";
    }

    @GetMapping("/login")
    final String loginForm() {
        return "login-registration/login";
    }

    @GetMapping("/logout")
    final String logout() {
        return "login-registration/logout";
    }

    @GetMapping("/login?expired")
    final String loginExpired() {
        return "login-registration/login_expired";
    }
}
