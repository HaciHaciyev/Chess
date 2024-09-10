package core.project.chess.infrastructure.utilities.chess;

import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.pieces.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class SimplePGNReader {
    private final String pgn;
    private final List<String> moves;
    private final Map<String, String> tags;

    public SimplePGNReader(String pgn) {
        String pgnStrip = pgn.strip();
        this.pgn = pgnStrip;
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
        List<String> moves = new ArrayList<>(movesArr.length - 1);
        for (String move : movesArr) {
            if (move.isEmpty() || move.isBlank()) {
                continue;
            }

            move = move.replaceAll("\\{.+}", " ");
            move = move.strip();
            move = move.replaceAll(" (\\w-\\w)|(1/2-1/2)", "");

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
        String move = rawMove.replaceAll("[+x#]", "");

        String[] splitMove = move.split(" ");


        PlayerMove white = splitMove[0].transform(s -> {
            String promotion = Character.isUpperCase(s.charAt(s.length() - 1)) ? String.valueOf(s.charAt(s.length() - 1)) : null;
            Piece promotionPiece = null;
            if (promotion != null) {
                s = s.substring(0, s.length() - 1);

                promotionPiece = switch (promotion) {
                    case "N" -> new Knight(Color.WHITE);
                    case "B" -> new Bishop(Color.WHITE);
                    case "R" -> new Rook(Color.WHITE);
                    case "Q" -> new Queen(Color.WHITE);
                    default -> null;
                };
            }



            String strStart = s.substring(0, 2);
            String strEnd = s.substring(2);

            Coordinate start = Coordinate.valueOf(strStart);
            Coordinate end = Coordinate.valueOf(strEnd);

            return new PlayerMove(start, end, promotionPiece);
        });

        PlayerMove black = null;

        if (splitMove.length > 1) {
            black = splitMove[1].transform(s -> {
                String promotion = Character.isUpperCase(s.charAt(s.length() - 1)) ? String.valueOf(s.charAt(s.length() - 1)) : null;
                Piece promotionPiece = null;
                if (promotion != null) {
                    s = s.substring(0, s.length() - 1);

                    promotionPiece = switch (promotion) {
                        case "N" -> new Knight(Color.BLACK);
                        case "B" -> new Bishop(Color.BLACK);
                        case "R" -> new Rook(Color.BLACK);
                        case "Q" -> new Queen(Color.BLACK);
                        default -> null;
                    };
                }

                String strStart = s.substring(0, 2);
                String strEnd = s.substring(2);

                Coordinate start = Coordinate.valueOf(strStart);
                Coordinate end = Coordinate.valueOf(strEnd);

                return new PlayerMove(start, end, promotionPiece);
            });
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
