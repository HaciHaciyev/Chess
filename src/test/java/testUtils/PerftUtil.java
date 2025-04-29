package testUtils;

import core.project.chess.domain.Perft.PerftTask;
import io.quarkus.logging.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerftUtil {

    public static void assertPerftDepth1(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(20L, nodes, "Nodes count mismatch");
    }

    public static void assertPerftDepth2(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(400L, nodes, "Nodes count mismatch");
    }

    public static void assertPerftDepth3(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(8_902L, nodes, "Nodes count mismatch");
    }

    public static void assertPerftDepth4(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(197_281L, nodes, "Nodes count mismatch");
    }

    public static void assertPerftDepth5(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(4_865_609L, nodes, "Nodes count mismatch");
    }

    public static void assertPerftDepth6(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(119_060_324L, nodes, "Nodes count mismatch");
    }

    public static void assertPerftDepth7(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(3_195_901_860L, nodes, "Nodes count mismatch");
    }

    public static void assertPerftDepth8(long nodes) {
        Log.infof("Count of nodes: %d.", nodes);
        assertEquals(84_998_978_956L, nodes, "Nodes count mismatch");
    }

    public static List<PerftTask> read_perft_tasks() {
        String path = "src/main/resources/pgn/perftsuite.epd";

        File file = new File(path);
        List<PerftTask> perftTasks = new ArrayList<>();

        try (var reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                PerftTask task = getPerftTask(line);
                perftTasks.add(task);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return perftTasks;
    }

    private static PerftTask getPerftTask(String line) {
        String[] parts = line.split(";");
        String fen = parts[0].trim();
        long[] values = new long[parts.length - 1];

        for (int i = 1; i < parts.length; i++) {
            values[i - 1] = Long.parseLong(parts[i].split(" ")[1]);
        }
        return new PerftTask(fen, values);
    }

    public static void analyze(String our, String their) {
        // Split FENs into components
        String[] parts1 = our.split(" ");
        String[] parts2 = their.split(" ");

        // Extract board states
        String board1 = parts1[0];
        String board2 = parts2[0];

        // Expand board notations
        char[][] expandedBoard1 = expandFENBoard(board1.split("/"));
        char[][] expandedBoard2 = expandFENBoard(board2.split("/"));

        // Maps to store positions of pieces before and after
        Map<Character, List<String>> piecesBefore = new HashMap<>();
        Map<Character, List<String>> piecesAfter = new HashMap<>();

        // Build position maps
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                char piece1 = expandedBoard1[rank][file];
                char piece2 = expandedBoard2[rank][file];

                String squareName = getSquareName(file, rank);

                if (piece1 != '.') {
                    piecesBefore.putIfAbsent(piece1, new ArrayList<>());
                    piecesBefore.get(piece1).add(squareName);
                }

                if (piece2 != '.') {
                    piecesAfter.putIfAbsent(piece2, new ArrayList<>());
                    piecesAfter.get(piece2).add(squareName);
                }
            }
        }

        // Print boards side by side with color-coded differences
        System.out.println("    OUR             THEIR");
        System.out.println("  abcdefgh         abcdefgh");
        for (int rank = 0; rank < 8; rank++) {
            System.out.print((8 - rank) + " ");

            // Print first board row
            for (int file = 0; file < 8; file++) {
                char piece1 = expandedBoard1[rank][file];
                char piece2 = expandedBoard2[rank][file];

                if (piece1 != piece2) {
                    if (piece1 == '.') {
                        // Piece added in board2
                        System.out.print("\u001B[42m" + piece1 + "\u001B[0m"); // Green background for empty space where
                        // piece was added
                    } else if (piece2 == '.') {
                        // Piece removed in board2
                        System.out.print("\u001B[41m" + piece1 + "\u001B[0m"); // Red background for removed piece
                    } else {
                        // Piece changed
                        System.out.print("\u001B[43m" + piece1 + "\u001B[0m"); // Yellow background for changed piece
                    }
                } else {
                    System.out.print(piece1); // Unchanged
                }
            }

            System.out.print("       " + (8 - rank) + " ");

            // Print second board row
            for (int file = 0; file < 8; file++) {
                char piece1 = expandedBoard1[rank][file];
                char piece2 = expandedBoard2[rank][file];

                if (piece1 != piece2) {
                    if (piece1 == '.') {
                        // Piece added in board2
                        System.out.print("\u001B[42m" + piece2 + "\u001B[0m"); // Green background for added piece
                    } else if (piece2 == '.') {
                        // Piece removed in board2
                        System.out.print("\u001B[41m" + piece2 + "\u001B[0m"); // Red background for empty space where
                        // piece was removed
                    } else {
                        // Piece changed
                        System.out.print("\u001B[43m" + piece2 + "\u001B[0m"); // Yellow background for changed piece
                    }
                } else {
                    System.out.print(piece2); // Unchanged
                }
            }

            System.out.println();
        }

        // Analyze piece movements and captures
        Set<Character> allPieces = new HashSet<>();
        allPieces.addAll(piecesBefore.keySet());
        allPieces.addAll(piecesAfter.keySet());

        System.out.println("\nDetailed Analysis:");

        // Detect castling
        boolean whiteKingSideCastle = detectCastling(
            expandedBoard1,
            expandedBoard2,
            'K',
            true
        );
        boolean whiteQueenSideCastle = detectCastling(
            expandedBoard1,
            expandedBoard2,
            'Q',
            true
        );
        boolean blackKingSideCastle = detectCastling(
            expandedBoard1,
            expandedBoard2,
            'k',
            false
        );
        boolean blackQueenSideCastle = detectCastling(
            expandedBoard1,
            expandedBoard2,
            'q',
            false
        );

        if (whiteKingSideCastle) {
            System.out.println("White castled kingside (O-O)");
        }
        if (whiteQueenSideCastle) {
            System.out.println("White castled queenside (O-O-O)");
        }
        if (blackKingSideCastle) {
            System.out.println("Black castled kingside (O-O)");
        }
        if (blackQueenSideCastle) {
            System.out.println("Black castled queenside (O-O-O)");
        }

        // Analyze piece changes
        for (char piece : allPieces) {
            String pieceName = getPieceName(piece);

            List<String> beforePositions = piecesBefore.getOrDefault(
                piece,
                new ArrayList<>()
            );
            List<String> afterPositions = piecesAfter.getOrDefault(
                piece,
                new ArrayList<>()
            );

            // Handle new pieces (like promotions)
            if (
                !piecesBefore.containsKey(piece) &&
                piecesAfter.containsKey(piece)
            ) {
                for (String pos : afterPositions) {
                    System.out.println(
                        "\u001B[32m" +
                        pieceName +
                        " appeared at " +
                        pos +
                        "\u001B[0m"
                    );
                }
                continue;
            }

            // Handle removed pieces
            if (
                piecesBefore.containsKey(piece) &&
                !piecesAfter.containsKey(piece)
            ) {
                for (String pos : beforePositions) {
                    System.out.println(
                        "\u001B[31m" +
                        pieceName +
                        " removed from " +
                        pos +
                        "\u001B[0m"
                    );
                }
                continue;
            }

            // Handle moved pieces (count changed)
            if (beforePositions.size() > afterPositions.size()) {
                // Some pieces captured
                List<String> remainingPositions = new ArrayList<>(
                    beforePositions
                );
                remainingPositions.removeAll(afterPositions);

                for (String pos : remainingPositions) {
                    System.out.println(
                        "\u001B[31m" +
                        pieceName +
                        " at " +
                        pos +
                        " was captured\u001B[0m"
                    );
                }
            } else if (beforePositions.size() < afterPositions.size()) {
                // Pieces added (promotion)
                List<String> newPositions = new ArrayList<>(afterPositions);
                newPositions.removeAll(beforePositions);

                for (String pos : newPositions) {
                    System.out.println(
                        "\u001B[32m" +
                        pieceName +
                        " appeared at " +
                        pos +
                        " (possibly from promotion)\u001B[0m"
                    );
                }
            } else if (
                beforePositions.size() == afterPositions.size() &&
                !new HashSet<>(beforePositions).equals(
                    new HashSet<>(afterPositions)
                )
            ) {
                // Same number of pieces but positions changed - piece moved
                if (beforePositions.size() == 1 && afterPositions.size() == 1) {
                    // Clear case of a single piece moving
                    System.out.println(
                        "\u001B[33m" +
                        pieceName +
                        " moved from " +
                        beforePositions.getFirst() +
                        " to " +
                        afterPositions.getFirst() +
                        "\u001B[0m"
                    );
                } else {
                    // Multiple pieces of same type moved - try to identify individual movements
                    Map<String, String> moves = identifyPieceMoves(
                        beforePositions,
                        afterPositions
                    );
                    for (Map.Entry<String, String> move : moves.entrySet()) {
                        System.out.println(
                            "\u001B[33m" +
                            pieceName +
                            " moved from " +
                            move.getKey() +
                            " to " +
                            move.getValue() +
                            "\u001B[0m"
                        );
                    }
                }
            }
        }

        // Compare other FEN components
        compareOtherFENParts(parts1, parts2);
    }

    private static boolean detectCastling(
        char[][] before,
        char[][] after,
        char king,
        boolean isWhite
    ) {
        int baseRank = isWhite ? 7 : 0;

        if (before[baseRank][4] == king && after[baseRank][4] != king) {
            // King-side castling
            if (after[baseRank][6] == king) {
                char rook = isWhite ? 'R' : 'r';
                return (
                    before[baseRank][7] == rook && after[baseRank][5] == rook
                );
            }
            // Queen-side castling
            else if (after[baseRank][2] == king) {
                char rook = isWhite ? 'R' : 'r';
                return (
                    before[baseRank][0] == rook && after[baseRank][3] == rook
                );
            }
        }
        return false;
    }

    private static Map<String, String> identifyPieceMoves(
        List<String> before,
        List<String> after
    ) {
        Map<String, String> moves = new HashMap<>();

        // Naive approach - match pieces by proximity
        for (String beforePos : before) {
            if (!after.contains(beforePos)) {
                // Find closest position in 'after' that's not in 'before'
                String closestAfterPos = findClosestPosition(
                    beforePos,
                    after,
                    before
                );
                if (closestAfterPos != null) {
                    moves.put(beforePos, closestAfterPos);
                }
            }
        }

        return moves;
    }

    private static String findClosestPosition(
        String fromPos,
        List<String> candidates,
        List<String> excluded
    ) {
        String closest = null;
        int minDistance = Integer.MAX_VALUE;

        int fromFile = fromPos.charAt(0) - 'a';
        int fromRank = '8' - fromPos.charAt(1);

        for (String candidate : candidates) {
            if (!excluded.contains(candidate)) {
                int toFile = candidate.charAt(0) - 'a';
                int toRank = '8' - candidate.charAt(1);

                int distance =
                    Math.abs(fromFile - toFile) + Math.abs(fromRank - toRank);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = candidate;
                }
            }
        }

        return closest;
    }

    private static String getSquareName(int file, int rank) {
        char fileChar = (char) ('a' + file);
        char rankChar = (char) ('8' - rank);
        return "" + fileChar + rankChar;
    }

    private static String getPieceName(char piece) {
        boolean isWhite = Character.isUpperCase(piece);
        String color = isWhite ? "White" : "Black";

        return switch (Character.toUpperCase(piece)) {
            case 'P' -> color + " pawn";
            case 'R' -> color + " rook";
            case 'N' -> color + " knight";
            case 'B' -> color + " bishop";
            case 'Q' -> color + " queen";
            case 'K' -> color + " king";
            default -> color + " piece";
        };
    }

    private static void compareOtherFENParts(String[] parts1, String[] parts2) {
        String[] partNames = {
            "Active color",
            "Castling availability",
            "En passant target",
            "Halfmove clock",
            "Fullmove number",
        };

        System.out.println("\nOther FEN differences:");

        for (int i = 1; i < Math.min(parts1.length, parts2.length); i++) {
            if (!parts1[i].equals(parts2[i])) {
                System.out.println(partNames[i - 1] + ":");

                if (i == 1) { // Active color
                    System.out.println(
                        "  Turn changed from " +
                        (parts1[i].equals("w") ? "White" : "Black") +
                        " to " +
                        (parts2[i].equals("w") ? "White" : "Black")
                    );
                } else if (i == 2) { // Castling
                    analyzeCastlingChanges(parts1[i], parts2[i]);
                } else if (i == 3) { // En passant
                    analyzeEnPassantChanges(parts1[i], parts2[i]);
                } else {
                    System.out.println(
                        "  Changed from " + parts1[i] + " to " + parts2[i]
                    );
                }
            }
        }
    }

    private static void analyzeCastlingChanges(
        String castling1,
        String castling2
    ) {
        Set<Character> rights1 = new HashSet<>();
        Set<Character> rights2 = new HashSet<>();

        for (char c : castling1.toCharArray()) {
            rights1.add(c);
        }

        for (char c : castling2.toCharArray()) {
            rights2.add(c);
        }

        // Rights lost
        for (char c : castling1.toCharArray()) {
            if (!rights2.contains(c)) {
                switch (c) {
                    case 'K' -> System.out.println(
                        "  White lost kingside castling rights"
                    );
                    case 'Q' -> System.out.println(
                        "  White lost queenside castling rights"
                    );
                    case 'k' -> System.out.println(
                        "  Black lost kingside castling rights"
                    );
                    case 'q' -> System.out.println(
                        "  Black lost queenside castling rights"
                    );
                }
            }
        }

        // Rights gained (unusual but possible in some variants)
        for (char c : castling2.toCharArray()) {
            if (!rights1.contains(c)) {
                switch (c) {
                    case 'K' -> System.out.println(
                        "  White gained kingside castling rights"
                    );
                    case 'Q' -> System.out.println(
                        "  White gained queenside castling rights"
                    );
                    case 'k' -> System.out.println(
                        "  Black gained kingside castling rights"
                    );
                    case 'q' -> System.out.println(
                        "  Black gained queenside castling rights"
                    );
                }
            }
        }
    }

    private static void analyzeEnPassantChanges(String ep1, String ep2) {
        if (ep1.equals("-") && !ep2.equals("-")) {
            System.out.println("  En passant target square set to " + ep2);
        } else if (!ep1.equals("-") && ep2.equals("-")) {
            System.out.println(
                "  En passant target square removed from " + ep1
            );
        } else if (!ep1.equals("-") && !ep2.equals("-")) {
            System.out.println(
                "  En passant target square changed from " + ep1 + " to " + ep2
            );
        }
    }

    private static char[][] expandFENBoard(String[] ranks) {
        char[][] board = new char[8][8];

        for (int i = 0; i < 8; i++) {
            String rank = ranks[i];
            int fileIndex = 0;

            for (int j = 0; j < rank.length(); j++) {
                char c = rank.charAt(j);

                if (Character.isDigit(c)) {
                    // Fill empty squares
                    int emptyCount = Character.getNumericValue(c);
                    for (int k = 0; k < emptyCount; k++) {
                        board[i][fileIndex++] = '.';
                    }
                } else {
                    // Place piece
                    board[i][fileIndex++] = c;
                }
            }
        }

        return board;
    }

    private static void printMovesInColumns(
        List<?> moves,
        List<String> outliers,
        int numColumns
    ) {
        int itemsPerColumn = (int) Math.ceil(
            (double) moves.size() / numColumns
        );

        for (int row = 0; row < itemsPerColumn; row++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < numColumns; col++) {
                int index = row + col * itemsPerColumn;
                if (index < moves.size()) {
                    String moveStr = moves.get(index).toString();
                    if (outliers.contains(moveStr)) {
                        // Apply highlighting for outliers
                        String formattedMove = moveStr + "◄"; // Using unicode symbol for better alignment
                        line.append(
                            String.format(
                                "\u001B[31m%-15s\u001B[0m",
                                formattedMove
                            )
                        );
                    } else {
                        line.append(String.format("%-15s", moveStr));
                    }
                } else {
                    line.append(String.format("%-15s", ""));
                }
            }
            System.out.println(line);
        }
    }

    // Extracted comparator methods for better readability
    private static int compareOurMoves(
        core.project.chess.domain.chess.value_objects.Move one,
        core.project.chess.domain.chess.value_objects.Move two
    ) {
        // Compare from column
        int result = Integer.compare(one.from().column(), two.from().column());
        if (result != 0) return result;

        // Compare from row
        result = Integer.compare(one.from().row(), two.from().row());
        if (result != 0) return result;

        // Compare to column
        result = Integer.compare(one.to().column(), two.to().column());
        if (result != 0) return result;

        // Compare to row
        return Integer.compare(one.to().row(), two.to().row());
    }
}
