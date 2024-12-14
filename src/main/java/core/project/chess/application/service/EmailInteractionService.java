package core.project.chess.application.service;

import core.project.chess.domain.subdomains.user.value_objects.Email;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EmailInteractionService {

    private final Mailer mailer;

    EmailInteractionService(Mailer mailer) {
        this.mailer = mailer;
    }

    public void sendToEmail(Email email, String link) {
        String subject = "Email confirmation";
        String body = String.format(
                """
                You must follow this link %s to verify your email to create an account.
                If you are not the one who used this email address, then simply ignore this link.
                The link will be available within 6 minutes.
                """,
                link
        );

        mailer.send(Mail.withText(email.email(), subject, body));
    }
}
