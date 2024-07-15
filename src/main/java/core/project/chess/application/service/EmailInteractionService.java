package core.project.chess.application.service;

import core.project.chess.domain.aggregates.user.entities.EmailConfirmationToken;
import core.project.chess.domain.aggregates.user.value_objects.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Properties;

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

// TODO: Tester class. Needs to be removed
@Component
@Configuration
class Tester {

    @Bean
    public JavaMailSender javaMailSender() {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);

        mailSender.setUsername("im615142@gmail.com");
        mailSender.setPassword("dntxaogsqeapcpsg");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }

    public static void main(String[] args) {
        try (
                var context = new AnnotationConfigApplicationContext(Tester.class)
        ){
            var mailSender = context.getBean("javaMailSender", JavaMailSender.class);

            SimpleMailMessage message = new SimpleMailMessage();

            String subject = "Email confirmation";
            String body = "This is your verification token:" + "\n\nBe careful! Don't send it to anyone.";


            message.setFrom("im615142@gmail.com");
            message.setTo("im615143@gmail.com");
            message.setSubject(subject);
            message.setText(body);


            mailSender.send(message);

            System.out.println("Mail sent to " + "im615143@gmail.com");
        }


    }
}
