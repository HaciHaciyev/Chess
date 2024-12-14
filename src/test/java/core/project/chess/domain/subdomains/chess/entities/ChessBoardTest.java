package core.project.chess.domain.subdomains.chess.entities;

import core.project.chess.domain.subdomains.chess.enumerations.Color;
import core.project.chess.domain.subdomains.chess.pieces.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@Disabled
class ChessBoardTest {

    @Test
    @Disabled("For single test of performance.")
    void chessBoardInitializationPerformanceTest() {
        for (int i = 0; i < 150_000; i++) {

            long startTime = System.nanoTime();

            ChessBoard.starndardChessBoard(UUID.randomUUID());

            long endTime = System.nanoTime();
            long duration = endTime - startTime;

            System.out.println(duration);

        }
    }

    @Test
    @Disabled("For single test of performance.")
    void getBoardArray() {
        for (int i = 0; i < 150_000; i++) {

            long startTime = System.nanoTime();

            var board = new Piece[8][8];

            board[0][0] = new Rook(Color.BLACK);
            board[0][7] = new Rook(Color.BLACK);
            board[7][0] = new Rook(Color.WHITE);
            board[7][7] = new Rook(Color.WHITE);
            // Place Knights
            board[0][1] = new Knight(Color.BLACK);
            board[0][6] = new Knight(Color.BLACK);
            board[7][1] = new Knight(Color.WHITE);
            board[7][6] = new Knight(Color.WHITE);
            // Place Bishops
            board[0][2] = new Bishop(Color.BLACK);
            board[0][5] = new Bishop(Color.BLACK);
            board[7][2] = new Bishop(Color.WHITE);
            board[7][5] = new Bishop(Color.WHITE);
            // Place Queens
            board[0][3] = new Queen(Color.BLACK);
            board[7][3] = new Queen(Color.WHITE);
            // Place Kings
            board[0][4] = new King(Color.BLACK);
            board[7][4] = new King(Color.WHITE);
            // Place Pawns
            for (int y = 0; y < 8; y++) {
                board[1][y] = new Pawn(Color.BLACK);
                board[6][y] = new Pawn(Color.WHITE);
            }

            long endTime = System.nanoTime();
            long duration = endTime - startTime;

            System.out.println(duration);
        }
    }
}