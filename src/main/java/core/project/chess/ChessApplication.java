package core.project.chess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@SpringBootApplication
public class ChessApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ChessApplication.class);
        springApplication.setAdditionalProfiles("dev");
        springApplication.run(args);
    }

    @Bean
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }
}
