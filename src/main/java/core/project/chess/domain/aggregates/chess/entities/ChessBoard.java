package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.value_objects.*;
import core.project.chess.infrastructure.utilities.StatusPair;
import jakarta.annotation.Nullable;
import lombok.Getter;
import org.springframework.data.util.Pair;

import java.util.*;

public class ChessBoard {
    private final @Getter UUID chessBoardId;
    private boolean isWhiteKingMoved;
    private boolean isBlackKingMoved;
    private final Map<Coordinate, Field> fieldMap = new HashMap<>();
    private final List<AlgebraicNotation> listOfAlgebraicNotations = new LinkedList<>();

    private ChessBoard(UUID chessBoardId, InitializationTYPE initializationTYPE) {
        Objects.requireNonNull(chessBoardId);
        Objects.requireNonNull(initializationTYPE);

        final boolean standardInit = initializationTYPE.equals(InitializationTYPE.STANDARD);
        if (standardInit) {
            standardInitializer();
        }

        this.chessBoardId = chessBoardId;
    }

    public static ChessBoard initialPosition(UUID chessBoardId) {
        return new ChessBoard(chessBoardId, InitializationTYPE.STANDARD);
    }

    public List<String> getListOfAlgebraicNotations() {
        return listOfAlgebraicNotations.stream().map(AlgebraicNotation::algebraicNotation).toList();
    }

    public Optional<Pair<Coordinate, Coordinate>> getLastMove() {
        AlgebraicNotation algebraicNotation = Objects.requireNonNullElse(listOfAlgebraicNotations.getLast(), null);

        if (algebraicNotation == null) {
            return Optional.empty();
        }
        return Optional.of(
                Pair.of(algebraicNotation.from, algebraicNotation.to)
        );
    }

    public Field getField(Coordinate coordinate) {
        Field field = fieldMap.get(coordinate);
        return new Field(field.getCoordinate(), field.pieceOptional().orElse(null));
    }

    public Optional<Color> pieceColor(Coordinate from) {
        return Optional.ofNullable(
                fieldMap.get(from).piece.color()
        );
    }

    public boolean isWhiteKingMoved() {
        if (isWhiteKingMoved) {
            return true;
        }
        return false;
    }

    private void whiteKingMoved() {
        isWhiteKingMoved = true;
    }

    public boolean isBlackKingMoved() {
        if (isBlackKingMoved) {
            return true;
        }
        return false;
    }

    private void blackKingMoved() {
        isBlackKingMoved = true;
    }

    private boolean kingSafetyAfterMove() {
        return false;
    }

    private boolean stalemate() {
        return false;
    }

    private boolean check() {
        return false;
    }

    private boolean checkMate() {
        return false;
    }

    Operations reposition(
            final Coordinate from, final Coordinate to, final @Nullable Piece inCaseOfPromotion
    ) {
        /** Preparation of necessary data and validation.*/
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (from.equals(to)) {
            throw new IllegalArgumentException("Invalid move.");
        }

        Field startField = fieldMap.get(from);
        Field endField = fieldMap.get(to);
        Piece piece = startField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        /** Delegate the operation to another method if necessary.*/
        if (AlgebraicNotation.isCastling(piece, from, to)) {
            return castling(from, to);
        }

        /** Validation.*/
        StatusPair<Operations> statusPair = piece.isValidMove(this, from, to);
        if (!statusPair.status() || !kingSafetyAfterMove()) {
            throw new IllegalArgumentException("Invalid move.");
        }

        /**Process operations from StatusPair. All validation need to be processed before that.*/
        Operations operation = statusPair.valueOrElseThrow();

        startField.removeFigure();

        if (operation.equals(Operations.CAPTURE)) {
            endField.removeFigure();
        }

        if (operation.equals(Operations.PROMOTION)) {
            if (!endField.isEmpty()) {
                endField.removeFigure();
            }

            endField.addFigure(inCaseOfPromotion);
        }

        endField.addFigure(piece);

        /** Recording the move made in algebraic notation and return used Operation.*/
        return recordingMoveMade(piece, from, to, operation, inCaseOfPromotion);
    }

    private Operations recordingMoveMade(
            Piece piece, Coordinate from, Coordinate to, Operations operation, Piece inCaseOfPromotion
    ) {
        if (checkMate()) {
            if (operation.equals(Operations.PROMOTION)) {
                listOfAlgebraicNotations.add(
                        AlgebraicNotation.of(piece, operation, from, to, inCaseOfPromotion, Operations.CHECKMATE)
                );
                return Operations.CHECKMATE;
            }

            listOfAlgebraicNotations.add(
                    AlgebraicNotation.of(piece, Operations.CHECKMATE, from, to, null, null)
            );
            return Operations.CHECKMATE;
        }

        if (stalemate()) {
            if (operation.equals(Operations.PROMOTION)) {
                listOfAlgebraicNotations.add(
                        AlgebraicNotation.of(piece, operation, from, to, inCaseOfPromotion, Operations.STALEMATE)
                );
                return Operations.STALEMATE;
            }

            listOfAlgebraicNotations.add(
                    AlgebraicNotation.of(piece, Operations.STALEMATE, from, to, null, null)
            );
            return Operations.STALEMATE;
        }

        if (check()) {
            if (operation.equals(Operations.PROMOTION)) {
                listOfAlgebraicNotations.add(
                        AlgebraicNotation.of(piece, operation, from, to, inCaseOfPromotion, Operations.CHECK)
                );
                return Operations.CHECK;
            }

            listOfAlgebraicNotations.add(
                    AlgebraicNotation.of(piece, Operations.CHECK, from, to, null, null)
            );
            return Operations.CHECK;
        }

        listOfAlgebraicNotations.add(
                AlgebraicNotation.of(piece, operation, from, to, null, null)
        );

        return operation;
    }

    private Operations castling(final Coordinate from, final Coordinate to) {
        /** Preparation of necessary data and validation.*/
        Field kingStartedField = fieldMap.get(from);
        Field kingEndField = fieldMap.get(to);
        Piece piece = kingStartedField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        if (!(piece instanceof King king)) {
            throw new IllegalStateException("Invalid method usage, check the documentation.");
        }

        StatusPair<Operations> statusPair = king.canCastle(this, kingStartedField, kingEndField);
        if (!statusPair.status() || !kingSafetyAfterMove()) {
            throw new IllegalArgumentException("Invalid move.");
        }

        /**Process operations from StatusPair. All validation need to be processed before that.*/
        if (piece.color().equals(Color.WHITE)) {
            whiteKingMoved();
        } else {
            blackKingMoved();
        }

        kingStartedField.removeFigure();
        kingEndField.addFigure(king);

        final boolean shortCasting = AlgebraicNotation.Castle.SHORT_CASTLING.equals(AlgebraicNotation.castle(to));
        if (shortCasting) {
            moveRookInShortCastling(to);
        } else {
            moveRookInLongCastling(to);
        }

        /** Recording the move made in algebraic notation.*/
        Operations operation = statusPair.valueOrElseThrow();

        if (checkMate()) {
            listOfAlgebraicNotations.add(
                    AlgebraicNotation.of(piece, Operations.CHECKMATE, from, to, null, null)
            );
            return Operations.CHECKMATE;
        }

        if (stalemate()) {
            listOfAlgebraicNotations.add(
                    AlgebraicNotation.of(piece, Operations.STALEMATE, from, to, null, null)
            );
            return Operations.STALEMATE;
        }

        if (check()) {
            listOfAlgebraicNotations.add(
                    AlgebraicNotation.of(piece, Operations.CHECK, from, to, null, null)
            );
            return Operations.CHECK;
        }

        listOfAlgebraicNotations.add(
                AlgebraicNotation.of(piece, operation, from, to, null, null)
        );

        return operation;
    }

    private void moveRookInShortCastling(final Coordinate to) {
        final boolean isWhiteCastling = to.getRow() == 1;

        if (isWhiteCastling) {
            Field startField = fieldMap.get(Coordinate.H1);
            Field endField = fieldMap.get(Coordinate.F1);
            Rook rook = (Rook) startField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

            startField.removeFigure();
            endField.addFigure(rook);
            return;
        }

        Field startField = fieldMap.get(Coordinate.H8);
        Field endField = fieldMap.get(Coordinate.F8);
        Rook rook = (Rook) startField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        startField.removeFigure();
        endField.addFigure(rook);
    }

    private void moveRookInLongCastling(final Coordinate to) {
        final boolean isWhiteCastling = to.getRow() == 1;

        if (isWhiteCastling) {
            Field startField = fieldMap.get(Coordinate.A1);
            Field endField = fieldMap.get(Coordinate.D1);
            Rook rook = (Rook) startField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

            startField.removeFigure();
            endField.addFigure(rook);
            return;
        }

        Field startField = fieldMap.get(Coordinate.A8);
        Field endField = fieldMap.get(Coordinate.D8);
        Rook rook = (Rook) startField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        startField.removeFigure();
        endField.addFigure(rook);
    }

    private enum InitializationTYPE {
        STANDARD, DURING_THE_GAME
    }

    public static class Field {
        private Piece piece;
        private final @Getter Coordinate coordinate;

        public Field(Coordinate coordinate, Piece piece) {
            Objects.requireNonNull(coordinate);
            this.coordinate = coordinate;
            this.piece = piece;
        }

        public boolean isEmpty() {
            return piece == null;
        }

        public Optional<Piece> pieceOptional() {
            if (piece == null) {
                return Optional.empty();
            }

            Color color = piece.color();
            return switch (piece) {
                case King _ -> Optional.of(new King(color));
                case Queen _ -> Optional.of(new Queen(color));
                case Rook _ -> Optional.of(new Rook(color));
                case Bishop _ -> Optional.of(new Bishop(color));
                case Knight _ -> Optional.of(new Knight(color));
                default -> Optional.of(new Pawn(color));
            };
        }

        private void removeFigure() {
            this.piece = null;
        }

        private void addFigure(final Piece piece) {
            if (this.piece == null) {
                this.piece = piece;
            }
        }
    }

    @Getter
    public enum Operations {
        PROMOTION("="),
        CAPTURE("X"),
        CHECK("+"),
        STALEMATE("."),
        CHECKMATE("#"),
        EMPTY("");

        private final String algebraicNotation;

        Operations(String algebraicNotation) {
            this.algebraicNotation = algebraicNotation;
        }
    }

    /** Not always valid. Only ChessBoard can can guarantee the validity of a given value object.
     * This class should be used only in ChessBoard.*/
    private record AlgebraicNotation(String algebraicNotation, Operations operation,
                                     Coordinate from, Coordinate to,
                                     Operations afterPromotion) {

        public static AlgebraicNotation of(
                Piece piece, Operations operation, Coordinate from, Coordinate to,
                @Nullable Piece inCaseOfPromotion, @Nullable Operations afterPromotion
        ) {
            Objects.requireNonNull(piece);
            Objects.requireNonNull(operation);
            Objects.requireNonNull(from);
            Objects.requireNonNull(to);

            final boolean isCastle = isCastling(piece, from, to);
            if (isCastle) {
                return new AlgebraicNotation(
                        String.format(castle(to).getAlgebraicNotation(), operation), operation, from, to, Operations.EMPTY
                );
            }

            if (operation.equals(Operations.PROMOTION)) {
                Objects.requireNonNull(inCaseOfPromotion);

                if (!(piece instanceof Pawn)) {
                    throw new IllegalStateException("Only pawn available for promotion.");
                }

                if (!Objects.isNull(afterPromotion)) {
                    String algebraicNotation = String.format("%s-%s=%s%s", from, to, pieceToType(inCaseOfPromotion), afterPromotion);
                    return new AlgebraicNotation(algebraicNotation, operation, from, to, afterPromotion);
                }

                String algebraicNotation = String.format("%s-%s=%s", from, to, pieceToType(inCaseOfPromotion));
                return new AlgebraicNotation(algebraicNotation, operation, from, to, Operations.EMPTY);
            }

            if (!Objects.isNull(afterPromotion) || !Objects.isNull(inCaseOfPromotion)) {
                throw new IllegalStateException("Invalid method usage, check documentation.");
            }

            if (operation.equals(Operations.EMPTY)) {
                if (piece instanceof Pawn) {
                    String algebraicNotation = String.format("%s-%s", from, to);
                    return new AlgebraicNotation(algebraicNotation, operation, from, to, Operations.EMPTY);
                }

                String algebraicNotation = String.format("%s %s-%s", pieceToType(piece), from, to);
                return new AlgebraicNotation(algebraicNotation, operation, from, to, Operations.EMPTY);
            }

            if (piece instanceof Pawn) {
                String algebraicNotation = String.format("%s %s %s", from, operation, to);
                return new AlgebraicNotation(algebraicNotation, operation, from, to, Operations.EMPTY);
            }

            String algebraicNotation = String.format("%s %s %s %s", pieceToType(piece), from, operation, to);
            return new AlgebraicNotation(algebraicNotation, operation, from, to, Operations.EMPTY);
        }

        private static String pieceToType(Piece piece) {
            return switch (piece) {
                case King _ -> "K";
                case Queen _ -> "Q";
                case Rook _ -> "R";
                case Bishop _ -> "B";
                case Knight _ -> "N";
                default -> "";
            };
        }

        public static Castle castle(Coordinate to) {
            final boolean isShortCasting = to.equals(Coordinate.G1) || to.equals(Coordinate.G8);
            if (isShortCasting) {
                return Castle.SHORT_CASTLING;
            }

            return Castle.LONG_CASTLING;
        }

        /**
         * This function can only be used to predetermine the user's intention to make castling,
         * However, this is by no means a final validation of this operation.
         */
        public static boolean isCastling(Piece piece, Coordinate from, Coordinate to) {
            final boolean isKing = (piece instanceof King);
            if (!isKing) {
                return false;
            }

            final boolean isValidKingPosition = from.equals(Coordinate.E1) || from.equals(Coordinate.E8);
            if (!isValidKingPosition) {
                return false;
            }

            final boolean isCastle = to.equals(Coordinate.C1) || to.equals(Coordinate.G1) ||
                    to.equals(Coordinate.C8) || to.equals(Coordinate.G8);
            if (!isCastle) {
                return false;
            }

            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AlgebraicNotation that = (AlgebraicNotation) o;
            return Objects.equals(algebraicNotation, that.algebraicNotation) && operation == that.operation &&
                    from == that.from && to == that.to && afterPromotion == that.afterPromotion;
        }

    @Override
        public String toString() {
            return algebraicNotation;
        }

        @Getter
        public enum Castle {
            SHORT_CASTLING("O-O"), LONG_CASTLING("O-O-O");

            private final String algebraicNotation;

            Castle(String algebraicNotation) {
                this.algebraicNotation = algebraicNotation;
            }
        }
    }

    private void standardInitializer() {
        fieldMap.put(
                Coordinate.A1, new Field(Coordinate.A1, new Rook(Color.WHITE))
        );
        fieldMap.put(
                Coordinate.B1, new Field(Coordinate.B1, new Knight(Color.WHITE))
        );
        fieldMap.put(
                Coordinate.C1, new Field(Coordinate.C1, new Bishop(Color.WHITE))
        );
        fieldMap.put(
                Coordinate.D1, new Field(Coordinate.D1, new Queen(Color.WHITE))
        );
        fieldMap.put(
                Coordinate.E1, new Field(Coordinate.E1, new King(Color.WHITE))
        );
        fieldMap.put(
                Coordinate.F1, new Field(Coordinate.F1, new Bishop(Color.WHITE))
        );
        fieldMap.put(
                Coordinate.G1, new Field(Coordinate.G1, new Knight(Color.WHITE))
        );
        fieldMap.put(
                Coordinate.H1, new Field(Coordinate.H1, new Rook(Color.WHITE))
        );
        fieldMap.put(
                Coordinate.A8, new Field(Coordinate.A8, new Rook(Color.BLACK))
        );
        fieldMap.put(
                Coordinate.B8, new Field(Coordinate.B8, new Knight(Color.BLACK))
        );
        fieldMap.put(
                Coordinate.C8, new Field(Coordinate.C8, new Bishop(Color.BLACK))
        );
        fieldMap.put(
                Coordinate.D8, new Field(Coordinate.D8, new Queen(Color.BLACK))
        );
        fieldMap.put(
                Coordinate.E8, new Field(Coordinate.E8, new King(Color.BLACK))
        );
        fieldMap.put(
                Coordinate.F8, new Field(Coordinate.F8, new Bishop(Color.BLACK))
        );
        fieldMap.put(
                Coordinate.G8, new Field(Coordinate.G8, new Knight(Color.BLACK))
        );
        fieldMap.put(
                Coordinate.H8, new Field(Coordinate.H8, new Rook(Color.BLACK))
        );

        for (Coordinate coordinate : Coordinate.values()) {
            final boolean fieldForWhitePawn = coordinate.getRow() == 2;
            if (fieldForWhitePawn) {
                fieldMap.put(
                        coordinate, new Field(coordinate, new Pawn(Color.WHITE))
                );
            }

            final boolean fieldForBlackPawn = coordinate.getRow() == 7;
            if (fieldForBlackPawn) {
                fieldMap.put(
                        coordinate, new Field(coordinate, new Pawn(Color.BLACK))
                );
            }

            final boolean fieldMustBeEmpty = coordinate.getRow() != 1 && coordinate.getRow() != 2 &&
                    coordinate.getRow() != 7 && coordinate.getRow() != 8;
            if (fieldMustBeEmpty) {
                fieldMap.put(
                        coordinate, new Field(coordinate, null)
                );
            }
        }
    }
}
