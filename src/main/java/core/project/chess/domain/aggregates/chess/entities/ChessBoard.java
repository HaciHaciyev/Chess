package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.value_objects.*;
import lombok.Getter;
import java.util.*;

public class ChessBoard {
    private final Map<Coordinate, Field> fieldMap = new HashMap<>();
    private final List<AlgebraicNotation> listOfAlgebraicNotations = new ArrayList<>();

    private ChessBoard(InitializationTYPE initializationTYPE) {
        final boolean standardInit = initializationTYPE.equals(InitializationTYPE.STANDARD);
        if (standardInit) {
            standardInitializer();
        }
    }

    public static ChessBoard initialPosition() {
        return new ChessBoard(InitializationTYPE.STANDARD);
    }

    void reposition(AlgebraicNotation algebraicNotation) {
        Objects.requireNonNull(algebraicNotation);

        Coordinate startPosition = algebraicNotation.from();
        Coordinate endPosition = algebraicNotation.to();

        if (startPosition.equals(endPosition)) {
            throw new IllegalArgumentException("Invalid move.");
        }

        Field field = fieldMap.get(startPosition);

        Piece piece = field.getPiece()
                .orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        /** TODO Complete validation*/
        piece.isValidMove(this, startPosition, endPosition);

        field.removeFigure();

        fieldMap.get(endPosition).addFigure(piece);

        listOfAlgebraicNotations.add(algebraicNotation);
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

        public boolean isEmpty() {
            return piece == null;
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
