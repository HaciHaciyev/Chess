package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.*;
import core.project.chess.domain.chess.util.ChessBoardNavigator;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation.Castle;
import core.project.chess.domain.chess.value_objects.KingStatus;
import core.project.chess.domain.chess.value_objects.Move;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations;
import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;

public final class King implements Piece {
    private final Color color;
    private final int index;

    private static final King WHITE_KING = new King(WHITE, 5);
    private static final King BLACK_KING = new King(BLACK, 11);
    static final long[] WHITE_KING_MOVES_CACHE = new long[64];
    static final long[] BLACK_KING_MOVES_CACHE = new long[64];
    static {
        for (int square = 0; square < 64; square++) {
            WHITE_KING_MOVES_CACHE[square] = generatePseudoValidKingMoves(square, WHITE);
            BLACK_KING_MOVES_CACHE[square] = generatePseudoValidKingMoves(square, BLACK);
        }
    }
    static final long[][] CHECKERS_BITBOARD_FOR_WHITE = new long[Checkers.values().length][64];
    static final long[][] CHECKERS_BITBOARD_FOR_BLACK = new long[Checkers.values().length][64];
    static {
        for (int square = 0; square < 64; square++) {
            CHECKERS_BITBOARD_FOR_WHITE[Checkers.PAWNS.ordinal()][square] = Pawn.of(BLACK).pawnsAttacks(square);
            CHECKERS_BITBOARD_FOR_BLACK[Checkers.PAWNS.ordinal()][square] = Pawn.of(WHITE).pawnsAttacks(square);

            long knightAttacks = Knight.of(WHITE).knightAttacks(square);
            CHECKERS_BITBOARD_FOR_WHITE[Checkers.KNIGHTS.ordinal()][square] = knightAttacks;
            CHECKERS_BITBOARD_FOR_BLACK[Checkers.KNIGHTS.ordinal()][square] = knightAttacks;

            long diagonalAttacks = Bishop.of(WHITE).bishopAttacks(square);
            CHECKERS_BITBOARD_FOR_WHITE[Checkers.DIAGONALS.ordinal()][square] = diagonalAttacks;
            CHECKERS_BITBOARD_FOR_BLACK[Checkers.DIAGONALS.ordinal()][square] = diagonalAttacks;

            long orthogonalAttacks = Rook.of(WHITE).rookAttacks(square);
            CHECKERS_BITBOARD_FOR_WHITE[Checkers.ORTHOGONALS.ordinal()][square] = orthogonalAttacks;
            CHECKERS_BITBOARD_FOR_BLACK[Checkers.ORTHOGONALS.ordinal()][square] = orthogonalAttacks;
        }
    }

    public static King of(Color color) {
        return color == WHITE ? WHITE_KING : BLACK_KING;
    }

    private King(Color color, int index) {
        this.color = color;
        this.index = index;
    }

    @Override
    public Color color() {
        return color;
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public Set<Operations> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        Piece endField = chessBoard.piece(to);
        if (!kingMove(chessBoard, from, to)) return null;
        if (!chessBoard.safeForKing(from, to)) return null;

        Set<Operations> setOfOperations = EnumSet.noneOf(Operations.class);
        final boolean opponentPieceInEndField = endField != null;
        if (opponentPieceInEndField) setOfOperations.add(Operations.CAPTURE);
        return setOfOperations;
    }

    boolean kingMove(ChessBoard chessBoard, Coordinate startField, Coordinate endField) {
        long ownPieces = chessBoard.pieces(color);
        long validMoves = color == WHITE ?
                WHITE_KING_MOVES_CACHE[startField.ordinal()] & ~ownPieces :
                BLACK_KING_MOVES_CACHE[startField.ordinal()] & ~ownPieces;
        return (validMoves & endField.bitMask()) != 0;
    }

    public boolean safeForKing(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        ChessBoardNavigator boardNavigator = chessBoard.navigator();
        Coordinate kingPosition = color.equals(WHITE) ? chessBoard.currentWhiteKingPosition() : chessBoard.currentBlackKingPosition();

        if (kingPosition == from) {
            if (chessBoard.isCastling(this, from, to)) return safeToCastle(boardNavigator, from, to);
            return !isFieldDangerousOrBlocked(boardNavigator, to, from);
        }

        return validatePieceMovementForKingSafety(boardNavigator, kingPosition, from, to);
    }

    public KingStatus kingStatus(final ChessBoard chessBoard, final @Nullable Move lastMove) {
        return checkOrMate(chessBoard.navigator(), lastMove);
    }

    public boolean stalemate(final ChessBoard chessBoard, final @Nullable Move lastMove) {
        ChessBoardNavigator navigator = chessBoard.navigator();
        Coordinate kingCoordinate = navigator.kingCoordinate(color);

        KingStatus kingStatus = chessBoard.kingStatus();
        if (kingStatus != null && (kingStatus.status() == Operations.CHECK ||
                kingStatus.status() == Operations.CHECKMATE)) return false;
        List<Coordinate> enemies = kingStatus != null ? kingStatus.enemiesAttackingTheKing() : check(navigator, lastMove);
        if (!enemies.isEmpty()) return false;

        List<Coordinate> surroundingFieldsOfKing = surroundingFields(kingCoordinate);
        final boolean isSurrounded = isSurrounded(surroundingFieldsOfKing, navigator, kingCoordinate);
        if (!isSurrounded) return false;

        Pawn pawn = Pawn.of(color);
        long pawnBitboard = chessBoard.bitboard(pawn);
        if (pawnBitboard != 0) return !pawn.isAtLeastOneMove(navigator.board());

        Knight knight = Knight.of(color);
        long knightBitboard = chessBoard.bitboard(knight);
        if (knightBitboard != 0) return !knight.isAtLeastOneMove(navigator.board());

        Bishop bishop = Bishop.of(color);
        long bishopBitboard = chessBoard.bitboard(bishop);
        if (bishopBitboard != 0) return !bishop.isAtLeastOneMove(navigator.board());

        Rook rook = Rook.of(color);
        long rookBitboard = chessBoard.bitboard(rook);
        if (rookBitboard != 0) return !rook.isAtLeastOneMove(navigator.board());

        Queen queen = Queen.of(color);
        long queenBitboard = chessBoard.bitboard(queen);
        if (queenBitboard != 0) return !queen.isAtLeastOneMove(navigator.board());
        return true;
    }

    public List<Move> allValidMoves(final ChessBoard chessBoard) {
        return allValidMoves(chessBoard, new ArrayList<>());
    }

    private List<Move> allValidMoves(final ChessBoard chessBoard, final List<Move> validMoves) {
        long kingBitboard = chessBoard.bitboard(this);
        long ownPieces = chessBoard.pieces(color);

        int fromIndex = Long.numberOfTrailingZeros(kingBitboard);
        long moves = color == WHITE ?
                WHITE_KING_MOVES_CACHE[fromIndex] & ~ownPieces :
                BLACK_KING_MOVES_CACHE[fromIndex] & ~ownPieces;
        while (moves != 0) {
            int toIndex = Long.numberOfTrailingZeros(moves);
            moves &= moves - 1;

            Coordinate from = Coordinate.byOrdinal(fromIndex);
            Coordinate to = Coordinate.byOrdinal(toIndex);
            if (chessBoard.isCastling(this, from, to)) {
                Castle castle = AlgebraicNotation.castle(to);
                if (!chessBoard.ableToCastling(color, castle)) continue;
                if (chessBoard.safeForKing(from, to)) validMoves.add(new Move(from, to, null));
            } else {
                if (chessBoard.safeForKing(from, to)) validMoves.add(new Move(from, to, null));
            }
        }

        return validMoves;
    }

    private List<Coordinate> check(ChessBoardNavigator boardNavigator, Move lastMove) {
        Coordinate kingCoordinate = boardNavigator.kingCoordinate(color);
        int kingSquare = kingCoordinate.ordinal();
        Color oppositeColor = color.opposite();
        long[][] checkersTable = color == WHITE ? CHECKERS_BITBOARD_FOR_WHITE : CHECKERS_BITBOARD_FOR_BLACK;

        List<Coordinate> enemies = new ArrayList<>(2);
        if (lastMove != null) {
            Coordinate from = lastMove.from();
            Coordinate to = lastMove.to();

            Coordinate possibleCheckDirection = checkDirection(boardNavigator, kingCoordinate, to);
            if (possibleCheckDirection != null) enemies.add(possibleCheckDirection);

            Coordinate secondPossibleCheckDirection = checkDirection(boardNavigator, kingCoordinate, from);
            if (secondPossibleCheckDirection != null) enemies.add(secondPossibleCheckDirection);
            return enemies;
        }
        return check(boardNavigator, oppositeColor, checkersTable, kingSquare, kingCoordinate, enemies);
    }

    private boolean isFieldDangerousOrBlocked(
            ChessBoardNavigator navigator,
            Coordinate pivot,
            Coordinate simulateIgnore) {

        Piece piece = navigator.board().piece(pivot);
        if (piece != null && piece.color() == color) return true;

        int pivotSquare = pivot.ordinal();
        long[][] checkersTable = color == WHITE ? CHECKERS_BITBOARD_FOR_WHITE : CHECKERS_BITBOARD_FOR_BLACK;
        long[] enemyPieces = allEnemyPieces(navigator, color.opposite());
        long checkersBitboard = 0L;
        checkersBitboard |= checkersTable[Checkers.PAWNS.ordinal()][pivotSquare] & enemyPieces[0];
        checkersBitboard |= checkersTable[Checkers.KNIGHTS.ordinal()][pivotSquare] & enemyPieces[1];
        checkersBitboard |= checkersTable[Checkers.DIAGONALS.ordinal()][pivotSquare] & (enemyPieces[2] | enemyPieces[4]);
        checkersBitboard |= checkersTable[Checkers.ORTHOGONALS.ordinal()][pivotSquare] & (enemyPieces[3] | enemyPieces[4]);

        while (checkersBitboard != 0L) {
            int attackerSquare = Long.numberOfTrailingZeros(checkersBitboard);
            checkersBitboard &= checkersBitboard - 1;
            Coordinate attacker = Coordinate.byOrdinal(attackerSquare);
            Piece attackerPiece = navigator.board().piece(attacker);
            switch (attackerPiece) {
                case Bishop b -> {
                    if (!clearPath(navigator.board(), pivot, attacker, simulateIgnore)) continue;
                    return true;
                }
                case Queen q -> {
                    if (!clearPath(navigator.board(), pivot, attacker, simulateIgnore)) continue;
                    return true;
                }
                case Rook r -> {
                    if (!clearPath(navigator.board(), pivot, attacker, simulateIgnore)) continue;
                    return true;
                }
                default -> {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Coordinate> check(
            ChessBoardNavigator navigator,
            Color oppositeColor,
            long[][] checkersTable,
            int kingSquare,
            Coordinate kingCoordinate,
            List<Coordinate> enemies) {

        long[] enemyPieces = allEnemyPieces(navigator, oppositeColor);
        long checkersBitboard = 0L;
        checkersBitboard |= checkersTable[Checkers.PAWNS.ordinal()][kingSquare] & enemyPieces[0];
        checkersBitboard |= checkersTable[Checkers.KNIGHTS.ordinal()][kingSquare] & enemyPieces[1];
        checkersBitboard |= checkersTable[Checkers.DIAGONALS.ordinal()][kingSquare] & (enemyPieces[2] | enemyPieces[4]);
        checkersBitboard |= checkersTable[Checkers.ORTHOGONALS.ordinal()][kingSquare] & (enemyPieces[3] | enemyPieces[4]);

        while (checkersBitboard != 0L) {
            int attackerSquare = Long.numberOfTrailingZeros(checkersBitboard);
            checkersBitboard &= checkersBitboard - 1;
            Coordinate attacker = Coordinate.byOrdinal(attackerSquare);
            Piece piece = navigator.board().piece(attacker);
            switch (piece) {
                case Bishop b -> {
                    if (!clearPath(navigator.board(), kingCoordinate, attacker)) continue;
                    enemies.add(attacker);
                }
                case Queen q -> {
                    if (!clearPath(navigator.board(), kingCoordinate, attacker)) continue;
                    enemies.add(attacker);
                }
                case Rook r -> {
                    if (!clearPath(navigator.board(), kingCoordinate, attacker)) continue;
                    enemies.add(attacker);
                }
                default -> enemies.add(attacker);
            }
            if (enemies.size() == 2) break;
        }
        return enemies;
    }

    private KingStatus checkOrMate(ChessBoardNavigator boardNavigator, Move lastMove) {
        List<Coordinate> enemies = check(boardNavigator, lastMove);
        if (enemies.isEmpty()) return new KingStatus(Operations.CONTINUE, enemies);

        Coordinate kingCoordinate = boardNavigator.kingCoordinate(color);

        if (enemies.size() == 1) {
            Coordinate fieldWithEnemy = enemies.getFirst();

            if (canEat(boardNavigator, fieldWithEnemy)) return new KingStatus(Operations.CHECK, enemies);
            if (canBlock(boardNavigator, kingCoordinate, fieldWithEnemy)) return new KingStatus(Operations.CHECK, enemies);

            Operations operation = isHaveSafetySurroundingField(boardNavigator, kingCoordinate) ? Operations.CHECK : Operations.CHECKMATE;
            return new KingStatus(operation, enemies);
        }

        if (kingMove(boardNavigator.board(), kingCoordinate, enemies.getFirst()) &&
                !isFieldDangerousOrBlockedForKing(boardNavigator, enemies.getFirst(), color))
            return new KingStatus(Operations.CHECK, enemies);

        if (kingMove(boardNavigator.board(), kingCoordinate, enemies.getLast()) &&
                !isFieldDangerousOrBlockedForKing(boardNavigator, enemies.getLast(), color))
            return new KingStatus(Operations.CHECK, enemies);

        Operations operation = isHaveSafetySurroundingField(boardNavigator, kingCoordinate) ? Operations.CHECK : Operations.CHECKMATE;
        return new KingStatus(operation, enemies);
    }

    private static long[] allEnemyPieces(ChessBoardNavigator navigator, Color oppositeColor) {
        return new long[]{
                navigator.board().bitboard(Pawn.of(oppositeColor)),
                navigator.board().bitboard(Knight.of(oppositeColor)),
                navigator.board().bitboard(Bishop.of(oppositeColor)),
                navigator.board().bitboard(Rook.of(oppositeColor)),
                navigator.board().bitboard(Queen.of(oppositeColor))
        };
    }

    private boolean isSurrounded(
            List<Coordinate> surroundingFieldsOfKing,
            ChessBoardNavigator navigator,
            Coordinate kingCoordinate) {
        for (Coordinate coordinate : surroundingFieldsOfKing) {
            if (!isFieldDangerousOrBlocked(navigator, coordinate, kingCoordinate)) return false;
        }
        return true;
    }

    private boolean isHaveSafetySurroundingField(ChessBoardNavigator boardNavigator, Coordinate kingCoordinate) {
        List<Coordinate> fields = surroundingFields(kingCoordinate);
        for (Coordinate field : fields) {
            if (!isFieldDangerousOrBlockedForKing(boardNavigator, field, color)) return true;
        }

        return false;
    }

    private boolean safeToCastle(ChessBoardNavigator navigator, Coordinate presentKingPosition, Coordinate futureKingPosition) {
        Castle castle;
        if (presentKingPosition.column() < futureKingPosition.column()) castle = Castle.SHORT_CASTLING;
        else castle = Castle.LONG_CASTLING;

        if (!navigator.board().ableToCastling(color, castle)) return false;

        KingStatus kingStatus = navigator.board().kingStatus();
        List<Coordinate> attackers = kingStatus != null ? kingStatus.enemiesAttackingTheKing() : check(navigator, null);
        if (!attackers.isEmpty()) return false;

        List<Coordinate> fieldsToCastle = navigator.castlingFields(castle, color);
        for (Coordinate field : fieldsToCastle) {
            if (field.column() == 5) continue;
            if (!processCastling(navigator, field)) return false;
        }

        return true;
    }

    private boolean processCastling(ChessBoardNavigator boardNavigator, Coordinate pivot) {
        Piece piece = boardNavigator.board().piece(pivot);
        if (piece != null && !(piece instanceof King)) return false;

        Color oppositeColor = color == WHITE ? BLACK : WHITE;

        List<Coordinate> pawns = boardNavigator.pawnsThreateningTheCoordinateOf(pivot, oppositeColor);
        for (Coordinate coordinate : pawns) {
            Piece pawn = boardNavigator.board().piece(coordinate);
            if (pawn.color() != this.color) return false;
        }

        List<Coordinate> knights = boardNavigator.knightAttackPositionsNonNull(pivot);
        for (Coordinate coordinate : knights) {
            Piece knight = boardNavigator.board().piece(coordinate);
            if (knight instanceof Knight && knight.color() != this.color) return false;
        }

        List<Coordinate> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), pivot);
        for (Coordinate coordinate : diagonalFields) {
            Piece pieceDiagonal = boardNavigator.board().piece(coordinate);
            if ((pieceDiagonal instanceof Bishop || pieceDiagonal instanceof Queen) && pieceDiagonal.color() != this.color) return false;
        }

        List<Coordinate> horizontalVertical = boardNavigator.occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), pivot);
        for (Coordinate hvField : horizontalVertical) {
            Piece pieceFromHV = boardNavigator.board().piece(hvField);
            if ((piece instanceof Rook || piece instanceof Queen) && pieceFromHV.color() != this.color) return false;
        }

        List<Coordinate> surroundings = surroundingFields(pivot);
        for (Coordinate surroundingField : surroundings) {
            Piece surroundingPiece = boardNavigator.board().piece(surroundingField);
            if (surroundingPiece instanceof King && surroundingPiece.color() != this.color) return false;
        }

        return true;
    }

    private boolean validatePieceMovementForKingSafety(
            ChessBoardNavigator boardNavigator,
            Coordinate kingPosition,
            Coordinate from,
            Coordinate to) {

        ChessBoard board = boardNavigator.board();
        Color oppositeColor = color.opposite();
        KingStatus kingStatus = board.kingStatus();

        List<Coordinate> attackers = kingStatus == null ?
                check(boardNavigator, null) :
                kingStatus.enemiesAttackingTheKing();

        if (attackers.size() == 2) return false;

        long fromBitmask = from.bitMask();
        long toBitmask = to.bitMask();
        long simulatedBitboard = board.whitePieces() | board.blackPieces();
        simulatedBitboard &= ~fromBitmask;
        simulatedBitboard |= toBitmask;
        long simulatedOpponentBitboard = simulateOpponentBitboard(board, to, toBitmask);

        Direction openedDirection = Direction.directionOf(kingPosition, from);
        Coordinate openedAttacker = openedDirection == null ? null : findXRayAttackerInDirection(
                openedDirection, kingPosition, from, simulatedBitboard, simulatedOpponentBitboard
        );
        if (openedAttacker != null) {
            Piece attackerPiece = board.piece(openedAttacker);
            if (openedDirection.isDiagonal()) {
                if (attackerPiece instanceof Bishop || attackerPiece instanceof Queen) return false;
            }
            else {
                if (attackerPiece instanceof Rook || attackerPiece instanceof Queen) return false;
            }
        }

        if (attackers.isEmpty()) return true;

        Coordinate opponentField = attackers.getFirst();
        Piece attacker = board.piece(opponentField);
        if (attacker instanceof Pawn) return isPawnEaten(to, opponentField, boardNavigator);
        if (attacker instanceof Knight) return to == opponentField;
        final boolean surround = Math.abs(kingPosition.row() - opponentField.row()) <= 1 &&
                Math.abs(kingPosition.column() - opponentField.column()) <= 1;
        if (surround) return to == opponentField;
        if (to == opponentField) return true;

        /** Check for attack blocking ability.*/
        Direction direction = Direction.directionOf(kingPosition, opponentField);

        if (direction.isVertical()) {
            if (to.column() != kingPosition.column()) return false;
            return kingPosition.row() < opponentField.row() ?
                    to.row() > kingPosition.row() && to.row() < opponentField.row() :
                    to.row() < kingPosition.row() && to.row() > opponentField.row();
        }

        if (direction.isHorizontal()) {
            if (to.row() != kingPosition.row()) return false;
            return kingPosition.column() < opponentField.column() ?
                    to.column() > kingPosition.column() && to.column() < opponentField.column() :
                    to.column() < kingPosition.column() && to.column() > opponentField.column();
        }

        if (Math.abs(to.row() - kingPosition.row()) != Math.abs(to.column() - kingPosition.column())) return false;

        final boolean isFromTop = kingPosition.row() < opponentField.row();
        if (isFromTop) {
            final boolean isFromTopRight = kingPosition.column() < opponentField.column();
            if (isFromTopRight) return kingPosition.row() < to.row() &&
                    to.row() < opponentField.row() &&
                    kingPosition.column() < to.column() &&
                    to.column() < opponentField.column();

            return kingPosition.row() < to.row() &&
                    to.row() < opponentField.row() &&
                    kingPosition.column() > to.column() &&
                    to.column() > opponentField.column();
        }

        final boolean isFromRight = kingPosition.column() < opponentField.column();
        if (isFromRight) return kingPosition.row() > to.row() &&
                to.row() > opponentField.row() &&
                kingPosition.column() < to.column() &&
                to.column() < opponentField.column();

        return kingPosition.row() > to.row() &&
                to.row() > opponentField.row() &&
                kingPosition.column() > to.column() &&
                to.column() > opponentField.column();
    }

    private long simulateOpponentBitboard(ChessBoard board, Coordinate to, long toBitmask) {
        long opponentPieces = board.pieces(color.opposite());
        if (to == board.enPassant()) return opponentPieces ^ board.enPassant().bitMask();
        return opponentPieces ^ toBitmask;
    }

    private static boolean isPawnEaten(Coordinate to, Coordinate opponentPawn, ChessBoardNavigator boardNavigator) {
        Coordinate enPassaunt = boardNavigator.board().enPassant();
        if (enPassaunt != null && enPassaunt == to) {
            if (opponentPawn == to) return true;
            if (opponentPawn.column() != to.column()) return false;

            int row = to.row();
            int opponentRow = opponentPawn.row();
            if (boardNavigator.board().piece(opponentPawn).color() == BLACK) return row - opponentRow == 1;
            return row - opponentRow == -1;
        }

        return opponentPawn.equals(to);
    }

    private boolean canEat(ChessBoardNavigator boardNavigator, Coordinate enemyField) {
        Piece enemyPiece = boardNavigator.board().piece(enemyField);
        Coordinate enPassaunt = boardNavigator.board().enPassant();

        if (enemyPiece instanceof Pawn && enPassaunt != null) {
            List<Coordinate> surroundedByEnPassauntPawns = boardNavigator.pawnsThreateningTheCoordinateOf(enPassaunt, color);
            for (Coordinate possiblePawn : surroundedByEnPassauntPawns) {
                if (safeForKing(boardNavigator.board(), possiblePawn, enPassaunt)) return true;
            }
        }

        List<Coordinate> pawnsThatPotentiallyCanEatEnemyPiece = boardNavigator.pawnsThreateningTheCoordinateOf(enemyField, color);
        for (Coordinate possiblePawn : pawnsThatPotentiallyCanEatEnemyPiece) {
            if (safeForKing(boardNavigator.board(), possiblePawn, enemyField)) return true;
        }

        List<Coordinate> knightsThatPotentiallyCanEatEnemyPiece = boardNavigator.knightAttackPositionsNonNull(enemyField);
        for (Coordinate knight : knightsThatPotentiallyCanEatEnemyPiece) {
            Piece piece = boardNavigator.board().piece(knight);

            final boolean isOurKnight = piece.color() == color && piece instanceof Knight;
            if (isOurKnight && safeForKing(boardNavigator.board(), knight, enemyField)) return true;
        }

        List<Coordinate> firstPiecesFromDiagonalVectors = boardNavigator
                .occupiedFieldsInDirections(Direction.diagonalDirections(), enemyField);

        for (Coordinate diagonalField : firstPiecesFromDiagonalVectors) {
            Piece piece = boardNavigator.board().piece(diagonalField);

            final boolean canEatFromDiagonalPosition = (piece instanceof Bishop || piece instanceof Queen) &&
                    piece.color() == color &&
                    safeForKing(boardNavigator.board(), diagonalField, enemyField);

            if (canEatFromDiagonalPosition) return true;
        }

        List<Coordinate> firstPiecesFromHorizontalAndVerticalVectors = boardNavigator
                .occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), enemyField);

        for (Coordinate horizontalVerticalField : firstPiecesFromHorizontalAndVerticalVectors) {
            Piece piece = boardNavigator.board().piece(horizontalVerticalField);

            final boolean canEatFromHorizontalAndVerticalPositions = (piece instanceof Rook || piece instanceof Queen) &&
                    piece.color() == color &&
                    safeForKing(boardNavigator.board(), horizontalVerticalField, enemyField);

            if (canEatFromHorizontalAndVerticalPositions) return true;
        }

        return false;
    }

    private boolean canBlock(ChessBoardNavigator boardNavigator, Coordinate pivot, Coordinate enemyField) {
        Piece opponentPiece = boardNavigator.board().piece(enemyField);
        if (opponentPiece instanceof Knight) return false;

        final boolean surround = Math.abs(pivot.row() - enemyField.row()) <= 1 && Math.abs(pivot.column() - enemyField.column()) <= 1;
        if (surround) return false;

        final boolean vertical = pivot.column() == enemyField.column() && pivot.row() != enemyField.row();
        List<Coordinate> path = boardNavigator.fieldsInPath(pivot, enemyField, false);

        for (Coordinate field : path) {
            if (!vertical && pawnCanBlock(boardNavigator, field)) return true;

            List<Coordinate> knights = boardNavigator.knightAttackPositionsNonNull(field);
            for (Coordinate knight : knights) {
                Piece piece = boardNavigator.board().piece(knight);

                final boolean isOurKnight = piece.color() == color && piece instanceof Knight;
                if (isOurKnight && safeForKing(boardNavigator.board(), knight, field)) return true;
            }

            List<Coordinate> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), field);
            for (Coordinate diagonalField : diagonalFields) {
                Piece piece = boardNavigator.board().piece(diagonalField);

                final boolean figureThatCanBlock = piece.color() == color && (piece instanceof Bishop || piece instanceof Queen);
                if (figureThatCanBlock && safeForKing(boardNavigator.board(), diagonalField, field)) return true;
            }

            List<Coordinate> horizontalVertical = boardNavigator
                    .occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), field);

            for (Coordinate horizontalVerticalField : horizontalVertical) {
                Piece piece = boardNavigator.board().piece(horizontalVerticalField);

                if (piece.color() == color &&  (piece instanceof Rook || piece instanceof Queen) &&
                        safeForKing(boardNavigator.board(), horizontalVerticalField, field)) return true;
            }
        }
        return false;
    }

    private boolean pawnCanBlock(ChessBoardNavigator boardNavigator, Coordinate field) {
        Coordinate potentialPawnThatCanBlockAttackBySimpleMove;
        if (color == WHITE) {
            potentialPawnThatCanBlockAttackBySimpleMove = Coordinate.of(field.row() - 1, field.column());
        } else {
            potentialPawnThatCanBlockAttackBySimpleMove = Coordinate.of(field.row() + 1, field.column());
        }

        if (potentialPawnThatCanBlockAttackBySimpleMove != null) {
            Piece possiblePawn = boardNavigator.board().piece(potentialPawnThatCanBlockAttackBySimpleMove);

            if (possiblePawn != null) {
                final boolean pawnCanBlock = possiblePawn.color() == color &&
                        possiblePawn instanceof Pawn &&
                        safeForKing(boardNavigator.board(), potentialPawnThatCanBlockAttackBySimpleMove, field);

                if (pawnCanBlock) return true;
            }
        }

        final boolean potentiallyCanBeBlockedByPawnPassage = field.row() == 5 && color == BLACK ||
                field.row() == 4 && color == WHITE;

        if (potentiallyCanBeBlockedByPawnPassage) {
            Coordinate potentialPawnCoordinate;
            Coordinate secondPassageCoordinate;

            if (color == WHITE) {
                potentialPawnCoordinate = Coordinate.of(2, field.column());
                secondPassageCoordinate = Coordinate.of(5, field.column());
            } else {
                potentialPawnCoordinate = Coordinate.of(7, field.column());
                secondPassageCoordinate = Coordinate.of(4, field.column());
            }

            final Piece potentialPawn = boardNavigator.board().piece(potentialPawnCoordinate);

            final boolean isFriendlyPawnExists = potentialPawn != null &&
                    potentialPawn.color() == color &&
                    potentialPawn instanceof Pawn;

            return isFriendlyPawnExists &&
                    clearPath(boardNavigator.board(), potentialPawnCoordinate, secondPassageCoordinate) &&
                    safeForKing(boardNavigator.board(), potentialPawnCoordinate, secondPassageCoordinate);
        }
        return false;
    }

    private boolean isFieldDangerousOrBlockedForKing(ChessBoardNavigator boardNavigator, Coordinate pivot, Color kingColor) {
        ChessBoard board = boardNavigator.board();
        Coordinate kingCoordinate = boardNavigator.kingCoordinate(kingColor);
        Piece pivotPiece = board.piece(pivot);
        Color oppositeColor = kingColor == WHITE ? BLACK : WHITE;

        final boolean blocked = pivotPiece != null && pivotPiece.color() == kingColor;
        if (blocked) return true;

        List<Coordinate> pawns = boardNavigator.pawnsThreateningTheCoordinateOf(pivot, oppositeColor);
        if (!pawns.isEmpty()) return true;

        List<Coordinate> knights = boardNavigator.knightAttackPositionsNonNull(pivot);
        for (Coordinate possibleKnight : knights) {
            Piece piece = board.piece(possibleKnight);
            if (piece.color() != kingColor && piece instanceof Knight) return true;
        }

        final boolean isKingOpposition = boardNavigator.findKingOpposition(pivot, this.color == WHITE ? BLACK : WHITE);
        if (isKingOpposition) return true;

        List<Coordinate> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(),
                pivot, kingCoordinate
        );

        for (Coordinate coordinate : diagonalFields) {
            Piece piece = board.piece(coordinate);
            if (piece.color() != kingColor && (piece instanceof Bishop || piece instanceof Queen)) return true;
        }

        List<Coordinate> horizontalVerticalFields = boardNavigator.occupiedFieldsInDirections(
                Direction.horizontalVerticalDirections(),
                pivot, kingCoordinate
        );

        for (Coordinate coordinate : horizontalVerticalFields) {
            Piece piece = board.piece(coordinate);
            if (piece.color() != kingColor && (piece instanceof Rook || piece instanceof Queen)) return true;
        }

        return false;
    }

    @Nullable
    private Coordinate checkDirection(
            ChessBoardNavigator navigator,
            Coordinate pivot,
            Coordinate to) {

        Piece piece = navigator.board().piece(to);
        if (piece != null) {
            if (piece.color() == color) return null;
            return checkEnemyInDirection(navigator, pivot, to, piece);
        }

        SimpleDirection direction = SimpleDirection.directionOf(pivot, to);
        if (direction == null) return null;
        Direction deepDirection = Direction.directionOf(pivot, to);

        Coordinate occupiedFieldInDirection = occupiedFieldInDirection(navigator.board(), deepDirection, pivot);
        if (occupiedFieldInDirection == null) return null;

        Piece opponentPiece = navigator.board().piece(occupiedFieldInDirection);
        if (opponentPiece.color() == color) return null;

        if (direction == SimpleDirection.VERTICAL || direction == SimpleDirection.HORIZONTAL) {
            if (!clearPath(navigator.board(), pivot, to)) return null;
            if (opponentPiece instanceof Rook || opponentPiece instanceof Queen) return to;
            return null;
        }

        if (!clearPath(navigator.board(), pivot, to)) return null;
        if (opponentPiece instanceof Bishop || opponentPiece instanceof Queen) return to;
        return null;
    }

    @Nullable
    private Coordinate checkEnemyInDirection(
            ChessBoardNavigator navigator,
            Coordinate pivot,
            Coordinate to,
            Piece piece) {

        int differenceOfRow = Math.abs(pivot.row() - to.row());
        int differenceOfColumn = Math.abs(pivot.column() - to.column());

        final boolean isKnightAttackPosition = (differenceOfRow == 2 && differenceOfColumn == 1) ||
                (differenceOfRow == 1 && differenceOfColumn == 2);
        if (isKnightAttackPosition) {
            if (piece instanceof Knight) return to;
            return null;
        }

        SimpleDirection direction = SimpleDirection.directionOf(pivot, to);
        if (direction == null) return null;

        final boolean isSurrounded = differenceOfRow == 1 && differenceOfColumn == 1;

        if (direction == SimpleDirection.VERTICAL || direction == SimpleDirection.HORIZONTAL) {
            if (isSurrounded) {
                if (piece instanceof Bishop || piece instanceof Queen) return to;
                return null;
            }
            if (!clearPath(navigator.board(), pivot, to)) return null;
            if (piece instanceof Rook || piece instanceof Queen) return to;
            return null;
        }

        if (isSurrounded) {
            if (piece instanceof  Pawn || piece instanceof Bishop || piece instanceof Queen) return to;
            return null;
        }
        if (!clearPath(navigator.board(), pivot, to)) return null;
        if (piece instanceof Bishop || piece instanceof Queen) return to;
        return null;
    }

    private static long generatePseudoValidKingMoves(int square, Color color) {
        long moves = 0L;
        int row = square / 8;
        int col = square % 8;

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int r = row + dr;
                int c = col + dc;
                if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                    int newSquare = r * 8 + c;
                    moves |= 1L << newSquare;
                }
            }
        }

        if (color == WHITE && square == 60) {
            moves |= 1L << 62;
            moves |= 1L << 58;
        }
        if (color == BLACK && square == 4) {
            moves |= 1L << 6;
            moves |= 1L << 2;
        }
        return moves;
    }
}