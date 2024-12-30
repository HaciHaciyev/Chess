package core.project.chess.infrastructure.utilities;

import core.project.chess.domain.chess.util.ChessNotationsValidator;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;

public class NotationFromPGNTest {


    @Test
    void notationFromPGN() {
        String pgn = getPGN("src/main/resources/pgn/canonicalPGN.pgn");
        System.out.println("RAW PGN: " + pgn);

        ChessNotationsValidator.notationFromPGN(pgn);
    }

    private String getPGN(String s) {
        try (var reader = new BufferedReader(new FileReader(s))) {
            return reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
