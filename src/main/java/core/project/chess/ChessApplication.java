package core.project.chess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChessApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ChessApplication.class);
        springApplication.setAdditionalProfiles("dev");
        springApplication.run(args);
    }

}
