package core.project.chess.application.service;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
public class EmailInteractionService {
    // TODO for Ilham & Nicat

    @Autowired
    private JavaMailSender mailSender;

    /** You can change signature if you need*/
    public void sendToEmail(
            Email userEmail,
            EmailConfirmationToken token
    ) {

        SimpleMailMessage message = new SimpleMailMessage();

        String subject = "Email confirmation";
        String body = "This is your verification token:\n" + token + "\n\nBe careful! Don't send it to anyone.";


        message.setFrom("im615142@gmail.com"); // TODO: Change the email address to another business address. It's also have to be done in application.properties
        message.setTo(userEmail.getEmailAddress());
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);

        System.out.println("Mail sent to " + userEmail.getEmailAddress());
    }
}