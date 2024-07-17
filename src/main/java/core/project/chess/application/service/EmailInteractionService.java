package core.project.chess.application.service;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailInteractionService {

    private static final String BUSINESS_EMAIL = "";

    private final JavaMailSender emailSender;

    public EmailInteractionService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    // TODO
    public void sendToEmail(Email userEmail, EmailConfirmationToken token) {
        String subject = "Email confirmation";
        String body = """
                You must follow this link to verify your email to create an account.
                If you are not the one who used this email address, then simply ignore this link.
                The link will be available within 6 minutes.
                """;

        var message = new SimpleMailMessage();
        message.setFrom(BUSINESS_EMAIL);
        message.setTo(userEmail.email());
        message.setSubject(subject);
        message.setText(body);

        emailSender.send(message);
        log.info("Token sent to : {}", userEmail.email());
    }
}

