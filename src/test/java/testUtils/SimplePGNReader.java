package testUtils;

import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.*;
import core.project.chess.domain.chess.value_objects.ChessMove;
import core.project.chess.domain.chess.value_objects.Move;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class SimplePGNReader {
    private final List<String> moves;
    private final Map<String, String> tags;

    public SimplePGNReader(String pgn) {
        String pgnStrip = pgn.strip();
        this.tags = extractTags(pgnStrip);
        this.moves = extractMoves(pgnStrip);
    }

    public static List<String> extractFromPGN(String path) {
        File file = new File(path);
        List<String> pgnList = new ArrayList<>();

        try (var reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            int emptyLineOccurence = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) emptyLineOccurence++;

                /* tags and moves in PGN are separated by new line character
                    as in: [Tag value]
                           [AnotherTag value]
                           \n
                           1. e4 e5 etc.

                   and PGNs themselves are also separated by new line
                   so it means that every second new line is separating not tags and moves but 2 PGNs
                 */
                if (!sb.isEmpty() && emptyLineOccurence == 2) {
                    emptyLineOccurence = 0;
                    pgnList.add(sb.toString());
                    sb.delete(0, sb.length());
                    continue;
                }

                sb.append(line).append("\n");
            }

            if (pgnList.isEmpty()) {
                pgnList.add(sb.toString());
            }

            if (!sb.isEmpty()) {
                pgnList.add(sb.toString());
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return pgnList;
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
        List<String> gameMoves = new ArrayList<>(movesArr.length - 1);

        for (int i = 1; i < movesArr.length; i++) {
            String move = movesArr[i].trim();

            move = move.strip();
            move = move.replaceAll(" (\\w-\\w)|(1/2-1/2)", "");

            gameMoves.add(move);
        }
        return gameMoves;
    }

    public String tag(String tag) {
        Objects.requireNonNull(tag);
        return tags.get(tag.toLowerCase());
    }

    private Move mapToPlayerMove(String s, Color color) {
        String promotion = Character.isUpperCase(s.charAt(s.length() - 1)) ? String.valueOf(s.charAt(s.length() - 1)) : null;
        Piece promotionPiece = null;
        if (promotion != null) {
            s = s.substring(0, s.length() - 1);

            promotionPiece = switch (promotion) {
                case "N" -> Knight.of(color);
                case "B" -> Bishop.of(color);
                case "R" -> Rook.of(color);
                case "Q" -> Queen.of(color);
                default -> null;
            };
        }

        String strStart = s.substring(0, 2);
        String strEnd = s.substring(2);

        Coordinate start = Coordinate.valueOf(strStart);
        Coordinate end = Coordinate.valueOf(strEnd);

        return new Move(start, end, promotionPiece);
    }

    public ChessMove read(int i) {
        String rawMove = moves.get(i);
        String move = rawMove.replaceAll("[+x#]", "");

        String[] splitMove = move.split(" ");

        Move white = splitMove[0].transform(s -> mapToPlayerMove(s, Color.WHITE));

        Move black = null;

        if (splitMove.length > 1) {
            black = splitMove[1].transform(s -> mapToPlayerMove(s, Color.BLACK));
        }

        return new ChessMove(white, black);
    }

    public List<ChessMove> readAll() {
        List<ChessMove> result = new ArrayList<>(moves.size());

        for (int i = 0; i < moves.size(); i++) {
            result.add(read(i));
        }

        return result;
    }
}
