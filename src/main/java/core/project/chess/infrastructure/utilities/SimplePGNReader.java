package core.project.chess.infrastructure.utilities;

import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SimplePGNReader {
    private final String pgn;
    private final List<String> moves;
    private final Map<String, String> tags;

    public SimplePGNReader(String pgn) {
        this.pgn = pgn;
        this.tags = extractTags(pgn);
        this.moves = extractMoves(pgn);
    }

    private Map<String, String> extractTags(String pgn) {
        return pgn.lines()
                .takeWhile(string -> (string.startsWith("[") && string.endsWith("]")))
                .map(string -> string.replaceAll("[\\[\\]]", ""))
                .collect(Collectors.toMap(
                        string -> {
                            int space = string.indexOf(" ");
                            return string.substring(0, space).toLowerCase();
                        },
                        string -> {
                            int space = string.indexOf(" ");
                            return string.substring(space + 1);
                        },
                        (string, string2) -> string,
                        LinkedHashMap::new)
                );
    }

    private List<String> extractMoves(String pgn) {
        // drop tags
        String strMoves = pgn.lines()
                .dropWhile(string -> string.startsWith("[") && string.endsWith("]") || string.isEmpty())
                .reduce("", (a, b) -> a +" " + b);

        String[] movesArr = strMoves.split("\\d+\\.");
        List<String> moves = new ArrayList<>(movesArr.length - 1);
        for (String move : movesArr) {
            if (move.isEmpty() || move.isBlank()) {
                continue;
            }

            move = move.replaceAll("\\{.+}", " ");
            move = move.strip();
            move = move.replaceAll(" \\w-\\w", "");

            moves.add(move);
        }
        return moves;
    }

    private List<String> extractWhiteMoves(List<String> moves, int i) {
        return moves.subList(0, i).stream().map(string -> string.split(" ")[0]).toList();
    }

    private List<String> extractBlackMoves(List<String> moves, int i) {
        return moves.subList(0, i).stream().map(string -> string.split(" ")[1]).toList();
    }

    public String tag(String tag) {
        Objects.requireNonNull(tag);
        return tags.get(tag.toLowerCase());
    }

    public void printTags() {
        System.out.println(tags);
    }

    public void printMoves() {
        System.out.println(moves);
    }


    public ChessMove read(int i) {
        String rawMove = moves.get(i);
        log.info("reading move#" + (i+1) + " -> " + rawMove);
        String move = rawMove.replaceAll("[+x#]", "");
        String[] splitMove = move.split(" ");

        PlayerMove white = splitMove[0].transform(s -> {
            String strStart = s.substring(0, 2).toUpperCase();
            String strEnd = s.substring(2).toUpperCase();

            Coordinate start = Coordinate.valueOf(strStart);
            Coordinate end = Coordinate.valueOf(strEnd);

            return new PlayerMove(start, end, null);
        });

        PlayerMove black = null;

        if (splitMove.length > 1) {
            black = splitMove[1].transform(s -> {
                String strStart = s.substring(0, 2).toUpperCase();
                String strEnd = s.substring(2).toUpperCase();

                Coordinate start = Coordinate.valueOf(strStart);
                Coordinate end = Coordinate.valueOf(strEnd);

                return new PlayerMove(start, end, null);
            });
        }

        return new ChessMove(white, black);
    }

    public List<ChessMove> readAll() {
        log.info("reading...");
        List<ChessMove> result = new ArrayList<>(moves.size());

        for (int i = 0; i < moves.size(); i++) {
            result.add(read(i));
        }

        log.info("done");
        return result;
    }
}
