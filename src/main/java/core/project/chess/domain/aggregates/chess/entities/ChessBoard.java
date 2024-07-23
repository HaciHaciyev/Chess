package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.value_objects.*;
import core.project.chess.infrastructure.utilities.StatusPair;
import lombok.Getter;
import java.util.*;

import static core.project.chess.domain.aggregates.chess.value_objects.AlgebraicNotation.*;

public class ChessBoard {
    private final Map<Coordinate, Field> fieldMap = new HashMap<>();
    private final List<AlgebraicNotation> listOfAlgebraicNotations = new LinkedList<>();

    private ChessBoard(InitializationTYPE initializationTYPE) {
        final boolean standardInit = initializationTYPE.equals(InitializationTYPE.STANDARD);
        if (standardInit) {
            standardInitializer();
        }
    }

    public static ChessBoard initialPosition() {
        return new ChessBoard(InitializationTYPE.STANDARD);
    }

    public List<AlgebraicNotation> getListOfAlgebraicNotations() {
        return List.copyOf(listOfAlgebraicNotations);
    }

    public Optional<Color> pieceColor(Coordinate from) {
        return Optional.ofNullable(
                fieldMap.get(from).piece.color()
        );
    }

    void reposition(final PieceTYPE pieceTYPE, final Coordinate from, final Coordinate to) {
        Objects.requireNonNull(pieceTYPE);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (from.equals(to)) {
            throw new IllegalArgumentException("Invalid move.");
        }

        if (AlgebraicNotation.isCastle(pieceTYPE, from, to)) {
            castling(pieceTYPE, from, to);
            return;
        }

        Field field = fieldMap.get(from);
        Piece piece = field.getPiece().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        StatusPair<Operations> statusPair = piece.isValidMove(this, from, to);
        final boolean isValidMove = statusPair.status();
        if (!isValidMove) {
            throw new IllegalArgumentException("Invalid move.");
        }

        field.removeFigure();
        fieldMap.get(to).addFigure(piece);

        /** TODO add a tail to algebraic notation which will be contains Operations*/
        listOfAlgebraicNotations.add(AlgebraicNotation.of(pieceTYPE, from, to));
    }

    private void castling(final PieceTYPE pieceTYPE, final Coordinate from, final Coordinate to) {
        Field field = fieldMap.get(from);
        King king = (King) field.getPiece()
                .orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        StatusPair<Operations> statusPair = king.isValidMove(this, from, to);
        final boolean isValidMove = statusPair.status();
        if (!isValidMove) {
            throw new IllegalArgumentException("Invalid move.");
        }

        field.removeFigure();
        fieldMap.get(to).addFigure(king);

        final boolean shortCasting = Castle.SHORT_CASTLING.equals(AlgebraicNotation.castle(to));
        if (shortCasting) {
            moveRookInShortCasting(to);
        } else {
            moveRookInLongCasting(to);
        }

        listOfAlgebraicNotations.add(AlgebraicNotation.of(pieceTYPE, from, to));
    }

    private void moveRookInShortCasting(Coordinate to) {
        if (to.getRow() == 1) {
            Field field = fieldMap.get(Coordinate.H1);
            Rook rook = (Rook) field.getPiece()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid move."));

            field.removeFigure();
            fieldMap.get(Coordinate.F1).addFigure(rook);
        } else {
            Field field = fieldMap.get(Coordinate.H8);
            Rook rook = (Rook) field.getPiece()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid move."));

            field.removeFigure();
            fieldMap.get(Coordinate.F8).addFigure(rook);
        }
    }

    private void moveRookInLongCasting(Coordinate to) {
        if (to.getRow() == 1) {
            Field field = fieldMap.get(Coordinate.A1);
            Rook rook = (Rook) field.getPiece()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid move."));

            field.removeFigure();
            fieldMap.get(Coordinate.D1).addFigure(rook);
        } else {
            Field field = fieldMap.get(Coordinate.A8);
            Rook rook = (Rook) field.getPiece()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid move."));

            field.removeFigure();
            fieldMap.get(Coordinate.D8).addFigure(rook);
        }
    }

    private enum InitializationTYPE {
        STANDARD, DURING_THE_GAME
    }

    @Getter
    private static class Field {
        private Piece piece;
        private final Coordinate coordinate;

        public Field(Coordinate coordinate, Piece piece) {
            Objects.requireNonNull(coordinate);
            this.coordinate = coordinate;
            this.piece = piece;
        }

        public Optional<Piece> getPiece() {
            Piece returnedType = piece;
            return Optional.ofNullable(returnedType);
        }

        public void removeFigure() {
            this.piece = null;
        }

        public void addFigure(final Piece piece) {
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
            final boolean fieldForBlackPawn = coordinate.getRow() == 7;
            final boolean fieldMustBeEmpty = coordinate.getRow() != 1 &&
                    coordinate.getRow() != 2 &&
                    coordinate.getRow() != 7 &&
                    coordinate.getRow() != 8;

            if (fieldForWhitePawn) {
                fieldMap.put(
                        coordinate, new Field(coordinate, new Pawn(Color.WHITE))
                );
            }

            if (fieldForBlackPawn) {
                fieldMap.put(
                        coordinate, new Field(coordinate, new Pawn(Color.BLACK))
                );
            }

            if (fieldMustBeEmpty) {
                fieldMap.put(
                        coordinate, new Field(coordinate, null)
                );
            }
        }
    }
}
