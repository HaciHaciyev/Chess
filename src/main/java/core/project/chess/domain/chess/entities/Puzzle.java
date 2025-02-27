package core.project.chess.domain.chess.entities;

import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.infrastructure.utilities.containers.Pair;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Puzzle {
    private final UUID puzzleId;
    private final ChessBoard chessBoard;
    private final AlgebraicNotation[] algebraicNotations;

    private int currentPosition;
    private boolean isHadMistake;
    private boolean isSolved;
    private boolean isEnded;

    public static Puzzle of(String pgn, int startPositionOfPuzzle) {
        Objects.requireNonNull(pgn);
        if (pgn.isBlank()) {
            throw new IllegalArgumentException("PGN can`t be blank.");
        }
        if (startPositionOfPuzzle < 0) {
            throw new IllegalArgumentException("Position index can`t be lower then 0.");
        }

        ChessBoard chessBoard = ChessBoard.fromPGN(pgn);
        AlgebraicNotation[] algebraicNotations = chessBoard.arrayOfAlgebraicNotations();

        if ((algebraicNotations.length - 1) >= startPositionOfPuzzle) {
            throw new IllegalArgumentException("Start position of puzzle can`t be greater or equal to size of halfmoves.");
        }

        int requiredMoveReturns = (algebraicNotations.length - 1) - startPositionOfPuzzle;
        while (requiredMoveReturns != 0) {
            chessBoard.returnOfTheMovement();
            requiredMoveReturns--;
        }

        return new Puzzle(UUID.randomUUID(), chessBoard, algebraicNotations);
    }

    public UUID ID() {
        return puzzleId;
    }

    public ChessBoard chessBoard() {
        return chessBoard;
    }

    public int currentPosition() {
        return currentPosition;
    }

    public boolean isSolved() {
        return isSolved;
    }

    public boolean isEnded() {
        return isEnded;
    }

    public AlgebraicNotation[] getAlgebraicNotations() {
        AlgebraicNotation[] copiedArray = new AlgebraicNotation[algebraicNotations.length];
        System.arraycopy(algebraicNotations, 0, copiedArray, 0, algebraicNotations.length);

        return copiedArray;
    }

    public Pair<String, Optional<String>> makeMovement(final Coordinate from, final Coordinate to, @Nullable final Piece inCaseOfPromotion) {
        if (this.isEnded) {
            throw new IllegalArgumentException("Puzzle already solved.");
        }

        if (!isProperMove(from, to, inCaseOfPromotion)) {
            this.isHadMistake = true;
            this.isSolved = false;
            throw new IllegalArgumentException("Bad move.");
        }

        final String representationAfterPlayerMove;
        try {
            chessBoard.reposition(from, to, inCaseOfPromotion);
            representationAfterPlayerMove = chessBoard.actualRepresentationOfChessBoard();

            final boolean isLastMove = currentPosition == algebraicNotations.length - 1;
            if (isLastMove) {
                this.isEnded = true;
                this.isSolved = !this.isHadMistake;
                return Pair.of(representationAfterPlayerMove, Optional.empty());
            }

            final AlgebraicNotation algebraicNotation = algebraicNotations[++currentPosition];
            final Pair<Coordinate, Coordinate> coordinates = chessBoard.coordinates(algebraicNotation);
            final Piece requiredPromotion = chessBoard.getInCaseOfPromotion(algebraicNotation);

            chessBoard.reposition(coordinates.getFirst(), coordinates.getSecond(), requiredPromotion);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unexpected exception. Make sure the pgn for initializing the chess problem is valid.");
        }

        return Pair.of(representationAfterPlayerMove, Optional.of(chessBoard.actualRepresentationOfChessBoard()));
    }

    private boolean isProperMove(Coordinate from, Coordinate to, Piece inCaseOfPromotion) {
        final AlgebraicNotation algebraicNotation = algebraicNotations[++currentPosition];
        final Pair<Coordinate, Coordinate> coordinates = chessBoard.coordinates(algebraicNotation);
        final Piece requiredPromotion = chessBoard.getInCaseOfPromotion(algebraicNotation);

        return from.equals(coordinates.getFirst()) && to.equals(coordinates.getSecond()) && inCaseOfPromotion.equals(requiredPromotion);
    }
}
