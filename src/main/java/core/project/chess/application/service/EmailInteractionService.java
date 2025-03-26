package core.project.chess.application.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

@ApplicationScoped
public class EmailInteractionService {

    private final Mailer mailer;

    EmailInteractionService(Instance<Mailer> mailer) {
        this.mailer = mailer.get();
    }

    public void sendToEmail(String email, String link) {
        String subject = "Email confirmation";
        String body = String.format(
                """
                You must follow this link %s to verify your email to create an account.
                If you are not the one who used this email address, then simply ignore this link.
                The link will be available within 6 minutes.
                """,
                link
        );

        mailer.send(Mail.withText(email, subject, body));
    }
}
