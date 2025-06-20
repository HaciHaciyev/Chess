package core.project.chess.domain.chess.entities;

import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.events.PuzzleGameResult;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.domain.commons.annotations.Nullable;
import core.project.chess.domain.commons.tuples.Pair;
import core.project.chess.domain.commons.util.Glicko2RatingCalculator;
import core.project.chess.domain.commons.value_objects.PuzzleStatus;
import core.project.chess.domain.commons.value_objects.Rating;
import core.project.chess.domain.commons.value_objects.RatingUpdateOnPuzzle;

import java.util.*;

public class Puzzle {
    private final UUID puzzleId;
    private Rating rating;
    private final ChessBoard chessBoard;
    private final AlgebraicNotation[] algebraicNotations;
    private final UUID player;
    private final String startPositionFEN;
    private final int startPositionIndex;
    private int currentPosition;
    private boolean isHadMistake;
    private boolean isSolved;
    private boolean isEnded;
    private final Deque<PuzzleGameResult> domainEvents = new ArrayDeque<>();

    public static final double USER_RATING_WINDOW = 150.00;

    private Puzzle(UUID puzzleId, Rating rating, ChessBoard chessBoard,
                   AlgebraicNotation[] algebraicNotations, UUID player, int startPositionIndex) {
        this.puzzleId = puzzleId;
        this.rating = rating;
        this.chessBoard = chessBoard;
        this.algebraicNotations = algebraicNotations;
        this.startPositionFEN = chessBoard.toString();
        this.player = player;
        this.startPositionIndex = startPositionIndex;
    }

    /**
     Only for saving, not for play.
     */
    public static Puzzle of(String pgn, int startPositionOfPuzzle) {
        if (pgn == null) throw new IllegalArgumentException("PGN is null. Puzzle requires PGN.");
        if (pgn.isBlank()) throw new IllegalArgumentException("PGN can`t be blank.");
        if (startPositionOfPuzzle < 0) throw new IllegalArgumentException("Position index can`t be lower than 0.");

        ChessBoard chessBoard = ChessBoard.fromPGN(pgn);
        AlgebraicNotation[] algebraicNotations = chessBoard.arrayOfAlgebraicNotations();

        if (chessBoard.countOfHalfMoves() >= startPositionOfPuzzle)
            throw new IllegalArgumentException("Start position of puzzle can`t be greater or equal than the size of halfmoves.");

        return new Puzzle(UUID.randomUUID(), Rating.defaultRating(), chessBoard, algebraicNotations, null, startPositionOfPuzzle);
    }

    public static Puzzle fromRepository(UUID id, UUID user, String pgn, int startPositionOfPuzzle, Rating rating) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(pgn);
        Objects.requireNonNull(user);
        Objects.requireNonNull(rating);
        if (pgn.isBlank()) throw new IllegalArgumentException("PGN can`t be blank.");
        if (startPositionOfPuzzle < 0) throw new IllegalArgumentException("Position index can`t be lower than 0.");

        ChessBoard chessBoard = ChessBoard.fromPGN(pgn);
        AlgebraicNotation[] algebraicNotations = chessBoard.arrayOfAlgebraicNotations();

        if ((algebraicNotations.length - 1) >= startPositionOfPuzzle)
            throw new IllegalArgumentException("Start position of puzzle can`t be greater or equal than the size of halfmoves.");

        int requiredMoveReturns = (algebraicNotations.length - 1) - startPositionOfPuzzle;
        while (requiredMoveReturns != 0) {
            chessBoard.undoMove();
            requiredMoveReturns--;
        }

        return new Puzzle(id, rating, chessBoard, algebraicNotations, user, startPositionOfPuzzle);
    }

    public UUID id() {
        return puzzleId;
    }

    public ChessBoard chessBoard() {
        return chessBoard;
    }

    public Rating rating() {
        return rating;
    }

    public UUID player() {
        return player;
    }

    public String startPositionFEN() {
        return startPositionFEN;
    }

    public int startPositionIndex() {
        return this.startPositionIndex;
    }

    public String fen() {
        return chessBoard.toString();
    }

    public String pgn() {
        final StringBuilder stringBuilder = new StringBuilder();

        int number = 1;
        for (int i = 0; i < algebraicNotations.length; i += 2) {
            final String notation = algebraicNotations[i].algebraicNotation();

            final String secondNotation;
            if (i + 1 <= algebraicNotations.length - 1) {
                secondNotation = algebraicNotations[i + 1].algebraicNotation();
            } else {
                secondNotation = "...";
            }

            stringBuilder.append(number)
                    .append(". ")
                    .append(notation)
                    .append(" ")
                    .append(secondNotation)
                    .append(" ");

            number++;
        }

        return stringBuilder.toString();
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

    public List<PuzzleGameResult> pullDomainEvents() {
        List<PuzzleGameResult> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public void changeRating(final RatingUpdateOnPuzzle ratingUpdate) {
        Objects.requireNonNull(ratingUpdate);

        final boolean doNotMatch = !ratingUpdate.gameID().equals(puzzleId);
        if (doNotMatch) {
            throw new IllegalArgumentException("Do not match id");
        }

        final double result = ratingUpdate.gameResult() == PuzzleStatus.UNSOLVED ? 1 : -1;
        this.rating = Glicko2RatingCalculator.calculate(this.rating, ratingUpdate.playerRating(), result);
    }

    public Pair<String, Optional<String>> makeMovement(final Coordinate from,
                                                       final Coordinate to,
                                                       @Nullable final Piece inCaseOfPromotion) {
        if (this.isEnded) {
            throw new IllegalArgumentException("Puzzle is already solved.");
        }

        if (!isProperMove(from, to, inCaseOfPromotion)) {
            this.isHadMistake = true;
            this.isSolved = false;
            throw new IllegalArgumentException("Bad move.");
        }

        final String representationAfterPlayerMove;
        try {
            chessBoard.doMove(from, to, inCaseOfPromotion);
            representationAfterPlayerMove = chessBoard.toString();

            final boolean isLastMove = currentPosition == algebraicNotations.length - 1;
            if (isLastMove) {
                puzzleOver();
                return Pair.of(representationAfterPlayerMove, Optional.empty());
            }

            final AlgebraicNotation algebraicNotation = algebraicNotations[++currentPosition];
            final Pair<Coordinate, Coordinate> coordinates = chessBoard.extractCoordinates(algebraicNotation);
            final Piece requiredPromotion = chessBoard.getInCaseOfPromotion(algebraicNotation);

            chessBoard.doMove(coordinates.getFirst(), coordinates.getSecond(), requiredPromotion);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unexpected exception. Make sure the pgn for initializing the chess problem is valid: " + e.getMessage());
        }

        return Pair.of(representationAfterPlayerMove, Optional.of(chessBoard.toString()));
    }

    private void puzzleOver() {
        this.isEnded = true;
        this.isSolved = !this.isHadMistake;

        PuzzleStatus puzzleStatus = this.isSolved ? PuzzleStatus.SOLVED : PuzzleStatus.UNSOLVED;
        this.domainEvents.add(new PuzzleGameResult(this.puzzleId, rating, puzzleStatus, player));
    }

    private boolean isProperMove(Coordinate from, Coordinate to, Piece inCaseOfPromotion) {
        final AlgebraicNotation algebraicNotation = algebraicNotations[++currentPosition];
        final Pair<Coordinate, Coordinate> coordinates = chessBoard.extractCoordinates(algebraicNotation);
        final Piece requiredPromotion = chessBoard.getInCaseOfPromotion(algebraicNotation);

        return from.equals(coordinates.getFirst()) && to.equals(coordinates.getSecond()) && inCaseOfPromotion.equals(requiredPromotion);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Puzzle puzzle)) return false;

        return startPositionIndex == puzzle.startPositionIndex &&
                currentPosition == puzzle.currentPosition &&
                isHadMistake == puzzle.isHadMistake &&
                isSolved == puzzle.isSolved &&
                isEnded == puzzle.isEnded &&
                Objects.equals(puzzleId, puzzle.puzzleId) &&
                Objects.equals(rating, puzzle.rating) &&
                chessBoard.equals(puzzle.chessBoard) &&
                Arrays.equals(algebraicNotations, puzzle.algebraicNotations) &&
                player.equals(puzzle.player) &&
                Objects.equals(startPositionFEN, puzzle.startPositionFEN);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(puzzleId);
        result = 31 * result + Objects.hashCode(rating);
        result = 31 * result + chessBoard.hashCode();
        result = 31 * result + Arrays.hashCode(algebraicNotations);
        result = 31 * result + Objects.hashCode(startPositionFEN);
        result = 31 * result + startPositionIndex;
        result = 31 * result + currentPosition;
        result = 31 * result + Boolean.hashCode(isHadMistake);
        result = 31 * result + Boolean.hashCode(isSolved);
        result = 31 * result + Boolean.hashCode(isEnded);
        return result;
    }
}
