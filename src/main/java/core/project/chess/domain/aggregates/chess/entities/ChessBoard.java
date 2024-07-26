package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.value_objects.*;
import core.project.chess.infrastructure.utilities.StatusPair;
import jakarta.annotation.Nullable;
import lombok.Getter;
import java.util.*;

import static core.project.chess.domain.aggregates.chess.value_objects.AlgebraicNotation.*;

public class ChessBoard {
    private final @Getter UUID chessBoardId;
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

    public List<AlgebraicNotation> getListOfAlgebraicNotations() {
        return List.copyOf(listOfAlgebraicNotations);
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

    Operations reposition(
            final Coordinate from, final Coordinate to, final @Nullable Piece inCaseOfPromotion
    ) {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (from.equals(to)) {
            throw new IllegalArgumentException("Invalid move.");
        }

        Field startField = fieldMap.get(from);
        Field endField = fieldMap.get(to);
        Piece piece = startField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        if (AlgebraicNotation.isCastling(piece, from, to)) {
            castling(from, to);
            return Operations.EMPTY;
        }

        StatusPair<Operations> statusPair = piece.isValidMove(this, from, to);
        final boolean isValidMove = statusPair.status();
        if (!isValidMove) {
            throw new IllegalArgumentException("Invalid move.");
        }

        /**Process operations from StatusPair. All validation need to be processed before that.*/

        Operations operation = statusPair.valueOrElse();

        startField.removeFigure();
        if (!endField.isEmpty() && operation.equals(Operations.CAPTURE)) {
            endField.removeFigure();
        }

        if (!endField.isEmpty()) {
            throw new IllegalArgumentException("Invalid move.");
        }

        if (operation.equals(Operations.PROMOTION)) {
            endField.addFigure(inCaseOfPromotion);
        } else {
            endField.addFigure(piece);
        }

        listOfAlgebraicNotations.add(
                AlgebraicNotation.of(piece, operation, from, to, null)
        );

        return operation;
    }

    private void castling(final Coordinate from, final Coordinate to) {
        final boolean shortCasting = Castle.SHORT_CASTLING.equals(AlgebraicNotation.castle(to));

        Field kingStartedField = fieldMap.get(from);
        Field kingEndField = fieldMap.get(to);
        Piece piece = kingStartedField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        if (!(piece instanceof King king)) {
            throw new IllegalArgumentException("Invalid move.");
        }

        StatusPair<Operations> statusPair = king.isValidMove(this, from, to);
        final boolean isValidMove = statusPair.status();
        if (!isValidMove) {
            throw new IllegalArgumentException("Invalid move.");
        }

        /**Process operations from StatusPair. All validation need to be processed before that.*/

        kingStartedField.removeFigure();
        kingEndField.addFigure(king);

        if (shortCasting) {
            moveRookInShortCastling(to);
        } else {
            moveRookInLongCastling(to);
        }

        listOfAlgebraicNotations.add(
                AlgebraicNotation.of(piece, Operations.EMPTY, from, to, null)
        );
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
