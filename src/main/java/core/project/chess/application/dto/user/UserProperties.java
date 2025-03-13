package core.project.chess.application.dto.user;

public record UserProperties(String firstname,
                             String surname,
                             String username,
                             String email,
                             double rating,
                             double bulletRating,
                             double blitzRating,
                             double rapidRating,
                             double puzzlesRating) {}