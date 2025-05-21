package core.project.chess.domain.chess.util;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.pieces.*;

import java.util.List;

public record ToStringUtils(ChessBoard board) {

    public String prettyToString() {
        String fen = board.toString();
        int space = fen.indexOf(" ");

        String boardRepresentation = fen.substring(0, space);
        int turn = board.countOfHalfMoves() + 1;
        String nextMove = fen.charAt(space + 1) == 'w' ? "White's turn" : "Black's turn";

        String whiteKingStatus = "NOT           ";
        String blackKingStatus = "NOT           ";

        String latestMovement;
        boolean movesMadePreviously = board.lastAlgebraicNotation().isPresent();

        if (movesMadePreviously) {
            latestMovement = board.lastAlgebraicNotation().orElseThrow().algebraicNotation();
        } else {
            latestMovement = "...     ";
        }

        List<Piece> whiteCaptures = board.whiteCaptures();
        List<Piece> blackCaptures = board.blackCaptures();

        StringBuilder view = new StringBuilder();
        String[] rows = boardRepresentation.split("/");

        String ANSI_GREEN = "\u001B[32m";
        String ANSI_RED = "\u001B[31m";
        String ANSI_DARK_RED = "\u001B[31m";
        String ANSI_RESET = "\u001B[0m";

        view.append("   A  B  C  D  E  F  G  H\n");
        for (int row = 0, rowIndex = 8; row < rows.length; row++, rowIndex--) {
            String strRow = rows[row];
            view.append(rowIndex).append(" [");
            char[] charRow = strRow.toCharArray();

            for (int columnIndex = 0; columnIndex < charRow.length; columnIndex++) {
                char c = charRow[columnIndex];
                if (Character.isLetter(c)) {
                    String prettyPiece = prettify(c, blackKingStatus, whiteKingStatus);

                    if (columnIndex == 0) {
                        view.append(prettyPiece);
                        continue;
                    }

                    view.append(". ").append(prettyPiece);
                }

                if (Character.isDigit(c)) {
                    if (columnIndex == 0) {
                        view.append(" ");
                        view.append(".  ".repeat(Character.getNumericValue(c) - 1));
                        continue;
                    }

                    view.append(".  ".repeat(Character.getNumericValue(c)));
                }
            }


            if (rowIndex == 8) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   | ")
                        .append('\t')
                        .append("FEN: ")
                        .append(fen)
                        .append('\n');

                continue;
            }

            if (rowIndex == 7) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   | ")
                        .append('\t')
                        .append("PGN: ")
                        .append(board.pgn())
                        .append('\n');

                continue;
            }

            if (rowIndex == 6) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   | ")
                        .append('\t')
                        .append("-".repeat(26))
                        .append('+')
                        .append("-".repeat(27))
                        .append('\n');

                continue;
            }

            if (rowIndex == 5) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   | ")
                        .append('\t')
                        .append("Turn: ")
                        .append(turn);

                if (turn < 10) {
                    view.append("\t\t\t\t")
                            .append("\t  |   ")
                            .append("Waiting for ")
                            .append(nextMove)
                            .append('\n');
                } else {
                    view.append("\t\t\t\t  ")
                            .append("|   ")
                            .append("Waiting for ")
                            .append(nextMove)
                            .append('\n');
                }

                continue;
            }

            if (rowIndex == 4) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   |\t")
                        .append("Move: ")
                        .append(latestMovement)
                        .append(" ".repeat(12))
                        .append("|   ")
                        .append('\n');

                continue;
            }

            if (rowIndex == 3) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   |\t")
                        .append("-".repeat(26))
                        .append("+")
                        .append("-".repeat(27))
                        .append('\n');

                continue;
            }

            if (rowIndex == 2) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   |\t")
                        .append("White king: ");

                if (whiteKingStatus.equals("SAFE")) {
                    whiteKingStatus = ANSI_GREEN + whiteKingStatus + "          " + ANSI_RESET;
                }
                if (whiteKingStatus.equals("CHECK")) {
                    whiteKingStatus = ANSI_RED + whiteKingStatus + "         " + ANSI_RESET;
                }
                if (whiteKingStatus.equals("CHECKMATE")) {
                    whiteKingStatus = ANSI_RED + whiteKingStatus + "     " + ANSI_RESET;
                }
                if (whiteKingStatus.equals("STALEMATE")) {
                    whiteKingStatus = ANSI_RED + whiteKingStatus + "     " + ANSI_RESET;
                }

                view.append(whiteKingStatus)
                        .append("|   ")
                        .append("White captures: [");

                for (Piece piece : whiteCaptures) {
                    view.append(pieceToPretty(piece));
                }
                view.append("]").append('\n');
                continue;
            }

            if (rowIndex == 1) {
                view.append("] ")
                        .append(rowIndex)
                        .append("   | ")
                        .append('\t')
                        .append("Black king: ");

                if (blackKingStatus.equals("SAFE")) {
                    blackKingStatus = ANSI_GREEN + blackKingStatus + "          " + ANSI_RESET;
                }
                if (blackKingStatus.equals("CHECK")) {
                    blackKingStatus = ANSI_RED + blackKingStatus + "         " + ANSI_RESET;
                }
                if (blackKingStatus.equals("CHECKMATE")) {
                    blackKingStatus = ANSI_RED + blackKingStatus + "     " + ANSI_RESET;
                }
                if (blackKingStatus.equals("STALEMATE")) {
                    blackKingStatus = ANSI_DARK_RED + blackKingStatus + "     " + ANSI_RESET;
                }

                view.append(blackKingStatus)
                        .append("|   ")
                        .append("Black captures: [");

                for (Piece piece : blackCaptures) {
                    view.append(pieceToPretty(piece));
                }

                view.append("]").append('\n');
                continue;
            }

            view.append("] ").append(rowIndex).append("   | ").append('\n');
        }

        view.append("   A  B  C  D  E  F  G  H      |    --------------------------+---------------------------");
        view.append('\n');

        return view.toString();
    }

    private String pieceToPretty(Piece piece) {
        return switch (piece) {
            case Pawn pawn -> pawn.color() == Color.WHITE ? "♟" : "♙";
            case Knight knight -> knight.color() == Color.WHITE ? "♞" : "♘";
            case Bishop bishop -> bishop.color() == Color.WHITE ? "♝" : "♗";
            case Rook rook -> rook.color() == Color.WHITE ? "♜" : "♖";
            case Queen queen -> queen.color() == Color.WHITE ? "♛" : "♕";
            default -> "";
        };
    }

    private static String prettify(char c, String blackKingStatus, String whiteKingStatus) {
        String ANSI_RED = "\u001B[31m";
        String ANSI_RESET = "\u001B[0m";

        String prettyPiece = switch (c) {
            case 'P' -> "♟";
            case 'N' -> "♞";
            case 'B' -> "♝";
            case 'R' -> "♜";
            case 'Q' -> "♛";
            case 'K' -> "♚";
            case 'p' -> "♙";
            case 'n' -> "♘";
            case 'b' -> "♗";
            case 'r' -> "♖";
            case 'q' -> "♕";
            case 'k' -> "♔";
            default -> "";
        };

        if ("♔".equals(prettyPiece) && blackKingStatus.equals("CHECK")) {
            prettyPiece = ANSI_RED + prettyPiece + ANSI_RESET;
        }

        if ("♔".equals(prettyPiece) && (blackKingStatus.equals("CHECKMATE") || blackKingStatus.equals("STALEMATE"))) {
            prettyPiece = ANSI_RED + prettyPiece + ANSI_RESET;
        }

        if ("♚".equals(prettyPiece) && whiteKingStatus.equals("CHECK")) {
            prettyPiece = ANSI_RED + prettyPiece + ANSI_RESET;
        }

        if ("♚".equals(prettyPiece) && (whiteKingStatus.equals("CHECKMATE") || whiteKingStatus.equals("STALEMATE"))) {
            prettyPiece = ANSI_RED + prettyPiece + ANSI_RESET;
        }

        return prettyPiece;
    }

    public static String prettyBitBoard(long bitboard) {
        String bits = Long.toBinaryString(bitboard);
        StringBuilder builder = new StringBuilder();

        if (bits.length() < 64) {
            int numZeros = 64 - bits.length();
            builder.append("0".repeat(numZeros));
            builder.append(bits);
        } else {
            builder.append(bits);
        }

        char[] bitsArray = builder.toString().toCharArray();
        builder.setLength(0);

        builder.append("\n");
        for (int row = 56; row >= 0; row -= 8) {
            builder.append("[");
            for (int col = 7; col >= 0; col--) {
                builder.append(bitsArray[row + col]);
                if (col != 0) {
                    builder.append(", ");
                }
            }
            builder.append("]");
            builder.append("\n");
        }
        return builder.toString();
    }
}
