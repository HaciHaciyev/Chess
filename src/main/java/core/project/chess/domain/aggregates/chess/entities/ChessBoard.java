package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.value_objects.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class ChessBoard {
    private final Set<Field> fieldSet;

    private ChessBoard(
            Set<Field> fieldSet, InitializationTYPE initializationTYPE
    ) {
        this.fieldSet = fieldSet;

        final boolean standardInit = initializationTYPE.equals(InitializationTYPE.STANDARD);
        if (standardInit) {
            standardInitializer();
        }
    }

    public static ChessBoard initialPosition() {
        return new ChessBoard(new HashSet<>(), InitializationTYPE.STANDARD);
    }

    private void standardInitializer() {
        setUpWhiteFigures();
        setUpBlackFigures();
        setUpPawnsAndEmptyFields();
    }

    private void setUpWhiteFigures() {
        fieldSet.add(
                new Field(Coordinate.A1, Optional.of(new Rook(Color.WHITE)))
        );
        fieldSet.add(
                new Field(Coordinate.B1, Optional.of(new Knight(Color.WHITE)))
        );
        fieldSet.add(
                new Field(Coordinate.C1, Optional.of(new Bishop(Color.WHITE)))
        );
        fieldSet.add(
                new Field(Coordinate.D1, Optional.of(new Queen(Color.WHITE)))
        );
        fieldSet.add(
                new Field(Coordinate.E1, Optional.of(new King(Color.WHITE)))
        );
        fieldSet.add(
                new Field(Coordinate.F1, Optional.of(new Bishop(Color.WHITE)))
        );
        fieldSet.add(
                new Field(Coordinate.G1, Optional.of(new Knight(Color.WHITE)))
        );
        fieldSet.add(
                new Field(Coordinate.H1, Optional.of(new Rook(Color.WHITE)))
        );
    }

    private void setUpBlackFigures() {
        fieldSet.add(
                new Field(Coordinate.A8, Optional.of(new Rook(Color.BLACK)))
        );
        fieldSet.add(
                new Field(Coordinate.B8, Optional.of(new Knight(Color.BLACK)))
        );
        fieldSet.add(
                new Field(Coordinate.C8, Optional.of(new Bishop(Color.BLACK)))
        );
        fieldSet.add(
                new Field(Coordinate.D8, Optional.of(new Queen(Color.BLACK)))
        );
        fieldSet.add(
                new Field(Coordinate.E8, Optional.of(new King(Color.BLACK)))
        );
        fieldSet.add(
                new Field(Coordinate.F8, Optional.of(new Bishop(Color.BLACK)))
        );
        fieldSet.add(
                new Field(Coordinate.G8, Optional.of(new Knight(Color.BLACK)))
        );
        fieldSet.add(
                new Field(Coordinate.H8, Optional.of(new Rook(Color.BLACK)))
        );
    }

    private void setUpPawnsAndEmptyFields() {
        for (Coordinate coordinate : Coordinate.values()) setUpRequireField(coordinate);
    }

    private void setUpRequireField(Coordinate coordinate) {
        boolean fieldForWhitePawn = coordinate.getRow() == 2;

        boolean fieldForBlackPawn = coordinate.getRow() == 7;

        boolean fieldMustBeEmpty = coordinate.getRow() != 1 &&
                coordinate.getRow() != 2 &&
                coordinate.getRow() != 7 &&
                coordinate.getRow() != 8;

        if (fieldForWhitePawn) {
            fieldSet.add(
                    new Field(coordinate, Optional.of(new Pawn(Color.WHITE)))
            );
        }

        if (fieldForBlackPawn) {
            fieldSet.add(
                    new Field(coordinate, Optional.of(new Pawn(Color.BLACK)))
            );
        }

        if (fieldMustBeEmpty) {
            fieldSet.add(
                    new Field(coordinate, Optional.empty())
            );
        }
    }

    private enum InitializationTYPE {
        STANDARD, DURING_THE_GAME
    }
}
