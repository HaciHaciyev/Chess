package core.project.chess.application.controller;

import core.project.chess.application.model.RegistrationForm;
import core.project.chess.application.service.EmailInteractionService;
import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import core.project.chess.domain.aggregates.user.value_objects.Password;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.utilities.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final PasswordEncoder passwordEncoder;

    private final InboundUserRepository inboundUserRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final EmailInteractionService emailInteractionService;

    @PostMapping("/registration")
    final String registration(@RequestBody RegistrationForm registrationForm) {
        if (!Objects.equals(
                registrationForm.password(), registrationForm.passwordConfirmation())
        ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        Username username = Result.ofThrowable(
                () -> new Username(registrationForm.username())
        ).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username")
        );

        Email email = Result.ofThrowable(
                () -> new Email(registrationForm.email())
        ).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email")
        );

        Password password = Result.ofThrowable(
                () -> new Password(passwordEncoder.encode(registrationForm.password()))
        ).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password")
        );

        Password passwordConfirmation = Result.ofThrowable(
                () -> new Password(passwordEncoder.encode(registrationForm.passwordConfirmation()))
        ).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password confirmation")
        );

        if (outboundUserRepository.isUsernameExists(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (outboundUserRepository.isEmailExists(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        UserAccount userAccount = Result.ofThrowable(() ->
                UserAccount.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(email)
                .password(password)
                .passwordConfirm(passwordConfirmation)
                .build()
        ).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.CONFLICT, "Invalid user account")
        );

        inboundUserRepository.save(userAccount);

        var token = EmailConfirmationToken.builder()
                .setTokenId(UUID.randomUUID())
                .setUserAccount(userAccount)
                .build();

        inboundUserRepository.saveUserToken(token);
        emailInteractionService.sendToEmail(email, token);

        return "redirect:/token/checking";
    }

    @GetMapping("/token/checking")
    final String tokenCheckingPage() {
        return "login-registration/token_check";
    }

    @PatchMapping("/token/verification/{tokenId}")
    // TODO
    final String tokenVerification(EmailConfirmationToken token)
            throws IllegalAccessException {
        if (token.isExpired()) {
            inboundUserRepository.deleteByToken(token);
            throw new IllegalAccessException("Token was expired.");
        }

        token.confirm();
        token.getUserAccount().enable();
        inboundUserRepository.enable(token);

        return "redirect:/login";
    }
}
