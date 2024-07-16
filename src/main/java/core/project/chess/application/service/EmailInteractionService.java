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

    public void sendToEmail(Email userEmail, EmailConfirmationToken token) {
        String subject = "Email confirmation";
        String body = String.format(
                "This is your verification token: \n %s \nBe careful! Don't send it to anyone.", token.token()
        );

        var message = new SimpleMailMessage();
        message.setFrom(BUSINESS_EMAIL);
        message.setTo(userEmail.email());
        message.setSubject(subject);
        message.setText(body);

        emailSender.send(message);
        log.info("Token sent to : {}", userEmail.email());
    }
}

