package core.project.chess.application.controller;

import core.project.chess.application.model.RegistrationForm;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Password;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserController {

    private final PasswordEncoder passwordEncoder;

    private final InboundUserRepository inboundUserRepository;

    @GetMapping("/home")
    final String home() {
        return "home";
    }

    @GetMapping("/registration")
    final String registrationForm() {
        return "registration";
    }

    @GetMapping("/login")
    final String loginForm() {
        return "login";
    }

    @GetMapping("/logout")
    final String logout() {return "logout";}

    @GetMapping("/login?expired")
    final String loginExpired() {return "login_expired";}

    @PostMapping("/registration")
    final String registration(@RequestBody RegistrationForm registrationForm) {
        if (!Objects.equals(registrationForm.password(), registrationForm.passwordConfirmation())) {
            // TODO
        }

        Password password = new Password(
                passwordEncoder.encode(registrationForm.password())
        );
        Password passwordConfirmation = new Password(
                passwordEncoder.encode(registrationForm.passwordConfirmation())
        );

        UserAccount userAccount = UserAccount.builder()
                .id(UUID.randomUUID())
                .username(new Username(registrationForm.username()))
                .email(new Email(registrationForm.email()))
                .password(password)
                .passwordConfirm(passwordConfirmation)
                .build();

        inboundUserRepository.save(userAccount);
        return "redirect:/login";
    }
}
