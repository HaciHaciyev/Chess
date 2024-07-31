//package core.project.chess.domain.aggregates.user.entities;
//
//import core.project.chess.domain.aggregates.user.events.AccountEvents;
//import core.project.chess.domain.aggregates.user.events.TokenEvents;
//import core.project.chess.domain.aggregates.user.value_objects.*;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//import java.util.concurrent.ThreadLocalRandom;
//
//
//@Slf4j
//class UserAccountTest {
//
//    @Test
//    void testUserAccountCreation() {
//        UserAccount defaultUserAccount = UserAccount.builder()
//                .id(UUID.randomUUID())
//                .username(new Username("User"))
//                .email(new Email("email@gmail.com"))
//                .password(new Password("password"))
//                .passwordConfirm(new Password("password"))
//                .build();
//
//        log.info(defaultUserAccount.toString());
//
//        short rating = 1869;
//        UserAccount userAccount = UserAccount.builder()
//                .id(UUID.randomUUID())
//                .username(new Username("Older user"))
//                .email(new Email("email@gmail.com"))
//                .password(new Password("password"))
//                .passwordConfirm(new Password("password"))
//                .rating(new Rating(rating))
//                .enable(true)
//                .accountEvents(AccountEvents.defaultEvents())
//                .build();
//
//        log.info(userAccount.toString());
//
//        try {
//            UserAccount defaultUserAccountWithNullValue = UserAccount.builder()
//                    .id(UUID.randomUUID())
//                    .username(null)
//                    .email(new Email("email@gmail.com"))
//                    .password(new Password("password"))
//                    .passwordConfirm(new Password("password"))
//                    .build();
//        } catch (NullPointerException e) {
//            log.info(e.getMessage());
//        }
//
//        try {
//            UserAccount illegalBuilding = UserAccount.builder()
//                    .id(UUID.randomUUID())
//                    .username(new Username("Older user"))
//                    .email(new Email("email@gmail.com"))
//                    .password(new Password("password"))
//                    .passwordConfirm(new Password("password"))
//                    /*Forgot a rating*/
//                    .enable(true)
//                    .accountEvents(AccountEvents.defaultEvents())
//                    .build();
//        } catch (IllegalArgumentException e) {
//            log.info(e.getMessage());
//        }
//
//        Password password = new Password(
//                String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000))
//        );
//
//        System.out.println(password);
//
//
//        EmailConfirmationToken emailConfirmationToken = EmailConfirmationToken.builder()
//                .tokenId(UUID.randomUUID())
//                .userAccount(userAccount)
//                .build();
//
//        TokenEvents tokenEvents = new TokenEvents(LocalDateTime.now());
//
//        EmailConfirmationToken emailConfirmationTokenSecond = EmailConfirmationToken.builder()
//                .tokenId(UUID.randomUUID())
//                .userAccount(userAccount)
//                .token(Token.createToken())
//                .tokenEvents(tokenEvents)
//                .confirmed(true)
//                .build();
//
//    }
//}