package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.*;
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
    public static final List<Coordinate> WSHORT_CASTLING_COORDINATES = List.of(Coordinate.e1, Coordinate.f1, Coordinate.g1);
    public static final List<Coordinate> WLONG_CASTLING_COORDINATES = List.of(Coordinate.e1, Coordinate.d1, Coordinate.c1);
    public static final List<Coordinate> BSHORT_CASTLING_COORDINATES = List.of(Coordinate.e8, Coordinate.f8, Coordinate.g8);
    public static final List<Coordinate> BLONG_CASTLING_COORDINATES = List.of(Coordinate.e8, Coordinate.d8, Coordinate.c8);
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
                WHITE_KING_MOVES_CACHE[startField.index()] & ~ownPieces :
                BLACK_KING_MOVES_CACHE[startField.index()] & ~ownPieces;
        return (validMoves & endField.bitMask()) != 0;
    }

    public boolean safeForKing(
            final ChessBoard chessBoard,
            final Coordinate from,
            final Coordinate to) {

        Coordinate kingPosition = color.equals(WHITE) ?
                chessBoard.currentWhiteKingPosition() :
                chessBoard.currentBlackKingPosition();

        if (kingPosition == from) {
            if (chessBoard.isCastling(this, from, to)) return safeToCastle(chessBoard, from, to);
            return !isFieldDangerousOrBlocked(chessBoard, to, from);
        }

        return validatePieceMovementForKingSafety(chessBoard, kingPosition, from, to);
    }

    public KingStatus kingStatus(
            final ChessBoard chessBoard,
            final @Nullable Move lastMove,
            final @Nullable Castle inCaseLastMoveIsCastle) {

        return checkOrMate(chessBoard, lastMove, inCaseLastMoveIsCastle);
    }

    public boolean stalemate(
            final ChessBoard chessBoard,
            final @Nullable Move lastMove,
            final @Nullable Castle inCaseLastMoveIsCastle) {

        Coordinate kingCoordinate = chessBoard.kingCoordinate(color);

        KingStatus kingStatus = chessBoard.kingStatus();
        if (kingStatus != null && (kingStatus.status() == Operations.CHECK ||
                kingStatus.status() == Operations.CHECKMATE)) return false;

        List<Coordinate> enemies = kingStatus != null ?
                kingStatus.enemiesAttackingTheKing() :
                check(chessBoard, lastMove, inCaseLastMoveIsCastle);
        if (!enemies.isEmpty()) return false;

        List<Coordinate> surroundingFieldsOfKing = surroundingFields(kingCoordinate);
        final boolean isSurrounded = isSurrounded(surroundingFieldsOfKing, chessBoard, kingCoordinate);
        if (!isSurrounded) return false;

        int countOfMoves = 0;

        Pawn pawn = Pawn.of(color);
        long pawnBitboard = chessBoard.bitboard(pawn);
        if (pawnBitboard != 0 && pawn.isAtLeastOneMove(chessBoard)) countOfMoves++;
        if (countOfMoves == 1) return false;

        Knight knight = Knight.of(color);
        long knightBitboard = chessBoard.bitboard(knight);
        if (knightBitboard != 0 && knight.isAtLeastOneMove(chessBoard)) countOfMoves++;
        if (countOfMoves == 1) return false;

        Bishop bishop = Bishop.of(color);
        long bishopBitboard = chessBoard.bitboard(bishop);
        if (bishopBitboard != 0 && bishop.isAtLeastOneMove(chessBoard)) countOfMoves++;
        if (countOfMoves == 1) return false;

        Rook rook = Rook.of(color);
        long rookBitboard = chessBoard.bitboard(rook);
        if (rookBitboard != 0 && rook.isAtLeastOneMove(chessBoard)) countOfMoves++;
        if (countOfMoves == 1) return false;

        Queen queen = Queen.of(color);
        long queenBitboard = chessBoard.bitboard(queen);
        if (queenBitboard != 0 && queen.isAtLeastOneMove(chessBoard)) countOfMoves++;
        return countOfMoves != 1;
    }

    public List<Move> allValidMoves(final ChessBoard chessBoard) {
        return allValidMoves(chessBoard, new ArrayList<>());
    }

    public List<Move> allValidMoves(final ChessBoard chessBoard, final List<Move> validMoves) {
        long kingBitboard = chessBoard.bitboard(this);
        long ownPieces = chessBoard.pieces(color);

        int fromIndex = Long.numberOfTrailingZeros(kingBitboard);
        long moves = color == WHITE ?
                WHITE_KING_MOVES_CACHE[fromIndex] & ~ownPieces :
                BLACK_KING_MOVES_CACHE[fromIndex] & ~ownPieces;
        while (moves != 0) {
            int toIndex = Long.numberOfTrailingZeros(moves);
            moves &= moves - 1;

            Coordinate from = Coordinate.byIndex(fromIndex);
            Coordinate to = Coordinate.byIndex(toIndex);
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

    private KingStatus checkOrMate(ChessBoard chessBoard, Move lastMove, Castle inCaseLastMoveIsCastle) {
        List<Coordinate> enemies = check(chessBoard, lastMove, inCaseLastMoveIsCastle);
        if (enemies.isEmpty()) return new KingStatus(Operations.CONTINUE, enemies);

        Coordinate kingCoordinate = chessBoard.kingCoordinate(color);

        if (enemies.size() == 1) {
            Coordinate fieldWithEnemy = enemies.getFirst();

            if (canEat(chessBoard, fieldWithEnemy)) return new KingStatus(Operations.CHECK, enemies);
            if (canBlock(chessBoard, kingCoordinate, fieldWithEnemy)) return new KingStatus(Operations.CHECK, enemies);

            Operations operation = isSurrounded(surroundingFields(kingCoordinate), chessBoard, kingCoordinate) ?
                    Operations.CHECKMATE :
                    Operations.CHECK;
            return new KingStatus(operation, enemies);
        }

        if (kingMove(chessBoard, kingCoordinate, enemies.getFirst()) &&
                !isFieldDangerousOrBlocked(chessBoard, enemies.getFirst(), kingCoordinate))
            return new KingStatus(Operations.CHECK, enemies);

        if (kingMove(chessBoard, kingCoordinate, enemies.getLast()) &&
                !isFieldDangerousOrBlocked(chessBoard, enemies.getLast(), kingCoordinate))
            return new KingStatus(Operations.CHECK, enemies);

        Operations operation = isSurrounded(surroundingFields(kingCoordinate), chessBoard, kingCoordinate) ?
                Operations.CHECKMATE :
                Operations.CHECK;
        return new KingStatus(operation, enemies);
    }

    private List<Coordinate> check(
            ChessBoard chessBoard,
            Move lastMove,
            Castle inCaseLastMoveIsCastle) {

        Coordinate kingCoordinate = chessBoard.kingCoordinate(color);
        int kingSquare = kingCoordinate.index();
        Color oppositeColor = color.opposite();
        long[][] checkersTable = color == WHITE ? CHECKERS_BITBOARD_FOR_WHITE : CHECKERS_BITBOARD_FOR_BLACK;

        List<Coordinate> enemies = new ArrayList<>(2);
        if (lastMove == null)
            return check(chessBoard, oppositeColor, checkersTable, kingSquare, kingCoordinate, enemies);

        Coordinate from = lastMove.from();
        Coordinate to = lastMove.to();

        Coordinate possibleCheckDirection = checkDirection(chessBoard, kingCoordinate, to);
        if (possibleCheckDirection != null) enemies.add(possibleCheckDirection);

        Coordinate secondPossibleCheckDirection = checkDirection(chessBoard, kingCoordinate, from);
        if (secondPossibleCheckDirection != null && secondPossibleCheckDirection != possibleCheckDirection)
            enemies.add(secondPossibleCheckDirection);

        if (inCaseLastMoveIsCastle != null) {
            Coordinate possibleRookCheckCoordinate = coordinateOfRookPositionInCastle(inCaseLastMoveIsCastle);
            Coordinate rookCheck = checkDirection(chessBoard, kingCoordinate, possibleRookCheckCoordinate);
            if (rookCheck != null) enemies.add(rookCheck);
        }

        return enemies;
    }

    private boolean isFieldDangerousOrBlocked(
            ChessBoard chessBoard,
            Coordinate pivot,
            Coordinate simulateIgnore) {

        Piece piece = chessBoard.piece(pivot);
        if (piece != null && piece.color() == color) return true;
        if (isOpponentKingOpposition(chessBoard, pivot)) return true;

        int pivotSquare = pivot.index();
        long[][] checkersTable = color == WHITE ? CHECKERS_BITBOARD_FOR_WHITE : CHECKERS_BITBOARD_FOR_BLACK;
        long[] enemyPieces = allEnemyPieces(chessBoard, color.opposite());
        long checkersBitboard = 0L;
        checkersBitboard |= checkersTable[Checkers.PAWNS.ordinal()][pivotSquare] & enemyPieces[0];
        checkersBitboard |= checkersTable[Checkers.KNIGHTS.ordinal()][pivotSquare] & enemyPieces[1];
        checkersBitboard |= checkersTable[Checkers.DIAGONALS.ordinal()][pivotSquare] & (enemyPieces[2] | enemyPieces[4]);
        checkersBitboard |= checkersTable[Checkers.ORTHOGONALS.ordinal()][pivotSquare] & (enemyPieces[3] | enemyPieces[4]);

        while (checkersBitboard != 0L) {
            int attackerSquare = Long.numberOfTrailingZeros(checkersBitboard);
            checkersBitboard &= checkersBitboard - 1;
            Coordinate attacker = Coordinate.byIndex(attackerSquare);
            Piece attackerPiece = chessBoard.piece(attacker);
            switch (attackerPiece) {
                case Bishop b -> {
                    if (!clearPath(chessBoard, pivot, attacker, simulateIgnore)) continue;
                    return true;
                }
                case Queen q -> {
                    if (!clearPath(chessBoard, pivot, attacker, simulateIgnore)) continue;
                    return true;
                }
                case Rook r -> {
                    if (!clearPath(chessBoard, pivot, attacker, simulateIgnore)) continue;
                    return true;
                }
                default -> {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isOpponentKingOpposition(ChessBoard chessBoard, Coordinate pivot) {
        Coordinate opponentKing = chessBoard.kingCoordinate(color.opposite());
        return Math.abs(pivot.row() - opponentKing.row()) <= 1 &&
                Math.abs(pivot.column() - opponentKing.column()) <= 1;
    }

    private List<Coordinate> check(
            ChessBoard chessBoard,
            Color oppositeColor,
            long[][] checkersTable,
            int kingSquare,
            Coordinate kingCoordinate,
            List<Coordinate> enemies) {

        long[] enemyPieces = allEnemyPieces(chessBoard, oppositeColor);
        long checkersBitboard = 0L;
        checkersBitboard |= checkersTable[Checkers.PAWNS.ordinal()][kingSquare] & enemyPieces[0];
        checkersBitboard |= checkersTable[Checkers.KNIGHTS.ordinal()][kingSquare] & enemyPieces[1];
        checkersBitboard |= checkersTable[Checkers.DIAGONALS.ordinal()][kingSquare] & (enemyPieces[2] | enemyPieces[4]);
        checkersBitboard |= checkersTable[Checkers.ORTHOGONALS.ordinal()][kingSquare] & (enemyPieces[3] | enemyPieces[4]);

        while (checkersBitboard != 0L) {
            int attackerSquare = Long.numberOfTrailingZeros(checkersBitboard);
            checkersBitboard &= checkersBitboard - 1;
            Coordinate attacker = Coordinate.byIndex(attackerSquare);
            Piece piece = chessBoard.piece(attacker);
            switch (piece) {
                case Bishop b -> {
                    if (!clearPath(chessBoard, kingCoordinate, attacker)) continue;
                    enemies.add(attacker);
                }
                case Queen q -> {
                    if (!clearPath(chessBoard, kingCoordinate, attacker)) continue;
                    enemies.add(attacker);
                }
                case Rook r -> {
                    if (!clearPath(chessBoard, kingCoordinate, attacker)) continue;
                    enemies.add(attacker);
                }
                default -> enemies.add(attacker);
            }
            if (enemies.size() == 2) break;
        }
        return enemies;
    }

    private boolean safeToCastle(
            ChessBoard chessBoard,
            Coordinate presentKingPosition,
            Coordinate futureKingPosition) {

        Castle castle;
        if (presentKingPosition.column() < futureKingPosition.column()) castle = Castle.SHORT_CASTLING;
        else castle = Castle.LONG_CASTLING;

        KingStatus kingStatus = chessBoard.kingStatus();
        List<Coordinate> attackers = kingStatus != null ?
                kingStatus.enemiesAttackingTheKing() :
                check(chessBoard, null, null);
        if (!attackers.isEmpty()) return false;

        final boolean castlePathClear = isCastlePathClear(chessBoard, castle);
        if (!castlePathClear) return false;

        List<Coordinate> fieldsToCastle = castlingFields(castle, color);
        for (int i = 1; i < fieldsToCastle.size(); i++) {
            Coordinate field = fieldsToCastle.get(i);
            if (isFieldDangerousOrBlocked(chessBoard, field, presentKingPosition)) return false;
        }

        return true;
    }

    private boolean isCastlePathClear(ChessBoard chessBoard, Castle castle) {
        if (castle == Castle.SHORT_CASTLING) {
            if (color == WHITE) return clearPath(chessBoard, Coordinate.e1, Coordinate.h1);
            return clearPath(chessBoard, Coordinate.e8, Coordinate.h8);
        }

        if (color == WHITE) return clearPath(chessBoard, Coordinate.e1, Coordinate.a1);
        return clearPath(chessBoard, Coordinate.e8, Coordinate.a8);
    }

    private boolean validatePieceMovementForKingSafety(
            ChessBoard chessBoard,
            Coordinate kingPosition,
            Coordinate from,
            Coordinate to) {

        Color oppositeColor = color.opposite();
        KingStatus kingStatus = chessBoard.kingStatus();

        List<Coordinate> attackers = kingStatus == null ?
                check(chessBoard, null, null) :
                kingStatus.enemiesAttackingTheKing();

        if (attackers.size() == 2) return false;

        long fromBitmask = from.bitMask();
        long toBitmask = to.bitMask();
        long simulatedBitboard = chessBoard.whitePieces() | chessBoard.blackPieces();
        simulatedBitboard &= ~fromBitmask;
        simulatedBitboard |= toBitmask;
        long simulatedOpponentBitboard = simulateOpponentBitboard(chessBoard, to, toBitmask);

        Direction openedDirection = Direction.directionOf(kingPosition, from);
        Coordinate openedAttacker = openedDirection == null ? null : findXRayAttackerInDirection(
                openedDirection, kingPosition, from, simulatedBitboard, simulatedOpponentBitboard
        );
        if (openedAttacker != null) {
            Piece attackerPiece = chessBoard.piece(openedAttacker);
            if (openedDirection.isDiagonal()) {
                if (attackerPiece instanceof Bishop || attackerPiece instanceof Queen) return false;
            }
            else {
                if (attackerPiece instanceof Rook || attackerPiece instanceof Queen) return false;
            }
        }

        if (attackers.isEmpty()) return true;

        Coordinate opponentField = attackers.getFirst();
        Piece attacker = chessBoard.piece(opponentField);
        if (attacker instanceof Pawn) return isPawnEaten(to, opponentField, chessBoard);
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

    private static long[] allEnemyPieces(ChessBoard chessBoard, Color oppositeColor) {
        return new long[]{
                chessBoard.bitboard(Pawn.of(oppositeColor)),
                chessBoard.bitboard(Knight.of(oppositeColor)),
                chessBoard.bitboard(Bishop.of(oppositeColor)),
                chessBoard.bitboard(Rook.of(oppositeColor)),
                chessBoard.bitboard(Queen.of(oppositeColor))
        };
    }

    private static boolean isPawnEaten(Coordinate to, Coordinate opponentPawn, ChessBoard chessBoard) {
        Coordinate enPassaunt = chessBoard.enPassant();
        if (enPassaunt != null && enPassaunt == to) {
            if (opponentPawn == to) return true;
            if (opponentPawn.column() != to.column()) return false;

            int row = to.row();
            int opponentRow = opponentPawn.row();
            if (chessBoard.piece(opponentPawn).color() == BLACK) return row - opponentRow == 1;
            return row - opponentRow == -1;
        }

        return opponentPawn.equals(to);
    }

    private boolean canEat(ChessBoard board, Coordinate target) {
        int square = target.index();
        long targetMask = target.bitMask();

        long ourPawns = board.bitboard(Pawn.of(color));
        long ourKnights = board.bitboard(Knight.of(color));
        long ourQueens = board.bitboard(Queen.of(color));
        long ourBishopsQueens = board.bitboard(Bishop.of(color)) | ourQueens;
        long ourRooksQueens = board.bitboard(Rook.of(color)) | ourQueens;

        long[][] table = color == WHITE ? CHECKERS_BITBOARD_FOR_BLACK : CHECKERS_BITBOARD_FOR_WHITE;

        if (pawnCanEat(target, board, table, ourPawns, square)) return true;
        if (knightCanTarget(target, table, square, ourKnights, board)) return true;
        return slidersCanTarget(target, table, square, ourBishopsQueens, board, ourRooksQueens);
    }

    private boolean canBlock(ChessBoard board, Coordinate pivot, Coordinate enemyField) {
        Piece opponentPiece = board.piece(enemyField);
        if (opponentPiece instanceof Knight) return false;

        final boolean surround = Math.abs(pivot.row() - enemyField.row()) <= 1 &&
                Math.abs(pivot.column() - enemyField.column()) <= 1;
        if (surround) return false;

        for (Coordinate target : fieldsInPath(pivot, enemyField)) {
            final boolean vertical = pivot.column() == enemyField.column() && pivot.row() != enemyField.row();
            if (!vertical && pawnCanBlock(board, target)) return true;

            int square = target.index();
            long targetMask = target.bitMask();
            long ourKnights = board.bitboard(Knight.of(color));
            long ourQueens = board.bitboard(Queen.of(color));
            long ourBishopsQueens = board.bitboard(Bishop.of(color)) | ourQueens;
            long ourRooksQueens = board.bitboard(Rook.of(color)) | ourQueens;

            long[][] table = color == WHITE ? CHECKERS_BITBOARD_FOR_BLACK : CHECKERS_BITBOARD_FOR_WHITE;
            if (knightCanTarget(target, table, square, ourKnights, board)) return true;
            if (slidersCanTarget(target, table, square, ourBishopsQueens, board, ourRooksQueens)) return true;
        }
        return false;
    }

    private boolean pawnCanEat(Coordinate target, ChessBoard board, long[][] table, long ourPawns, int square) {
        if (enPassant(target, board, table, ourPawns)) return true;

        long pawnAttackers = table[Checkers.PAWNS.ordinal()][square] & ourPawns;
        while (pawnAttackers != 0) {
            int fromMask = Long.numberOfTrailingZeros(pawnAttackers);
            Coordinate from = Coordinate.byIndex(fromMask);
            if (safeForKing(board, from, target)) return true;
            pawnAttackers &= pawnAttackers - 1;
        }
        return false;
    }

    private boolean enPassant(Coordinate target, ChessBoard board, long[][] table, long ourPawns) {
        Coordinate enPassant = board.enPassant();
        if (enPassant != null && board.piece(target) instanceof Pawn) {
            if (validateEnPassantAbility(target, enPassant)) return false;
            long enPassantAttackersMask = table[Checkers.PAWNS.ordinal()][enPassant.index()] & ourPawns;
            while (enPassantAttackersMask != 0) {
                int fromMask = Long.numberOfTrailingZeros(enPassantAttackersMask);
                if (safeForKing(board, Coordinate.byIndex(fromMask), enPassant)) return true;
                enPassantAttackersMask &= enPassantAttackersMask - 1;
            }
        }
        return false;
    }

    private boolean validateEnPassantAbility(Coordinate target, Coordinate enPassant) {
        if (enPassant.column() == target.column()) {
            if (color == WHITE) if (enPassant.row() - target.row() != 1) return true;
            if (color == BLACK) return enPassant.row() - target.row() != -1;
        }
        return false;
    }

    private boolean knightCanTarget(Coordinate target, long[][] table, int square, long ourKnights, ChessBoard board) {
        long knightAttackers = table[Checkers.KNIGHTS.ordinal()][square] & ourKnights;
        while (knightAttackers != 0) {
            int fromMask = Long.numberOfTrailingZeros(knightAttackers);
            if (safeForKing(board, Coordinate.byIndex(fromMask), target)) return true;
            knightAttackers &= knightAttackers - 1;
        }
        return false;
    }

    private boolean slidersCanTarget(
            Coordinate target, long[][] table, int square,
            long ourBishopsQueens, ChessBoard board, long ourRooksQueens) {

        long diagonalAttackers = table[Checkers.DIAGONALS.ordinal()][square] & ourBishopsQueens;
        while (diagonalAttackers != 0) {
            int fromMask = Long.numberOfTrailingZeros(diagonalAttackers);
            Coordinate from = Coordinate.byIndex(fromMask);
            if (clearPath(board, from, target) &&
                    safeForKing(board, from, target)) return true;
            diagonalAttackers &= diagonalAttackers - 1;
        }

        long orthogonalAttackers = table[Checkers.ORTHOGONALS.ordinal()][square] & ourRooksQueens;
        while (orthogonalAttackers != 0) {
            int fromMask = Long.numberOfTrailingZeros(orthogonalAttackers);
            Coordinate from = Coordinate.byIndex(fromMask);
            if (clearPath(board, from, target) &&
                    safeForKing(board, from, target)) return true;
            orthogonalAttackers &= orthogonalAttackers - 1;
        }
        return false;
    }

    private boolean pawnCanBlock(ChessBoard chessBoard, Coordinate field) {
        Coordinate canBlockByOnePush;
        if (color == WHITE) canBlockByOnePush = Coordinate.of(field.row() - 1, field.column());
        else canBlockByOnePush = Coordinate.of(field.row() + 1, field.column());
        if (canBlockByOnePush != null) {
            Piece possiblePawn = chessBoard.piece(canBlockByOnePush);
            if (possiblePawn != null) {
                final boolean pawnCanBlock = possiblePawn.color() == color &&
                        possiblePawn instanceof Pawn &&
                        safeForKing(chessBoard, canBlockByOnePush, field);
                if (pawnCanBlock) return true;
            }
        }

        final boolean canBlockByPassage = field.row() == 5 && color == BLACK ||
                field.row() == 4 && color == WHITE;
        if (canBlockByPassage) {
            Coordinate potentialPawnCoordinate;
            Coordinate secondPassageCoordinate;
            if (color == WHITE) {
                potentialPawnCoordinate = Coordinate.of(2, field.column());
                secondPassageCoordinate = Coordinate.of(5, field.column());
            } else {
                potentialPawnCoordinate = Coordinate.of(7, field.column());
                secondPassageCoordinate = Coordinate.of(4, field.column());
            }

            final Piece potentialPawn = chessBoard.piece(potentialPawnCoordinate);
            final boolean isFriendlyPawnExists = potentialPawn != null &&
                    potentialPawn.color() == color &&
                    potentialPawn instanceof Pawn;
            return isFriendlyPawnExists &&
                    clearPath(chessBoard, potentialPawnCoordinate, secondPassageCoordinate) &&
                    safeForKing(chessBoard, potentialPawnCoordinate, secondPassageCoordinate);
        }
        return false;
    }

    private boolean isSurrounded(
            List<Coordinate> surroundingFieldsOfKing,
            ChessBoard chessBoard,
            Coordinate kingCoordinate) {
        for (Coordinate coordinate : surroundingFieldsOfKing) {
            if (!isFieldDangerousOrBlocked(chessBoard, coordinate, kingCoordinate)) return false;
        }
        return true;
    }

    private List<Coordinate> castlingFields(AlgebraicNotation.Castle castle, Color color) {
        if (color == Color.WHITE) {
            if (castle == AlgebraicNotation.Castle.SHORT_CASTLING) return WSHORT_CASTLING_COORDINATES;
            return WLONG_CASTLING_COORDINATES;
        }

        if (castle == AlgebraicNotation.Castle.SHORT_CASTLING) return BSHORT_CASTLING_COORDINATES;
        return BLONG_CASTLING_COORDINATES;
    }

    private long simulateOpponentBitboard(ChessBoard board, Coordinate to, long toBitmask) {
        long opponentPieces = board.pieces(color.opposite());
        if (to == board.enPassant()) return opponentPieces ^ board.enPassant().bitMask();
        return opponentPieces ^ toBitmask;
    }

    private Coordinate coordinateOfRookPositionInCastle(Castle castle) {
        if (color == WHITE) {
            if (castle == Castle.SHORT_CASTLING) return Coordinate.BLACK_ROOK_SHORT_CASTLE_END;
            return Coordinate.BLACK_ROOK_LONG_CASTLE_END;
        }

        if (castle == Castle.SHORT_CASTLING) return Coordinate.WHITE_ROOK_SHORT_CASTLE_END;
        return Coordinate.WHITE_ROOK_LONG_CASTLE_END;
    }

    @Nullable
    private Coordinate checkDirection(
            ChessBoard chessBoard,
            Coordinate pivot,
            Coordinate to) {

        Piece piece = chessBoard.piece(to);
        if (piece != null) {
            if (piece.color() == color) return null;
            return checkEnemyInDirection(chessBoard, pivot, to, piece);
        }

        SimpleDirection direction = SimpleDirection.directionOf(pivot, to);
        if (direction == null) return null;
        Direction deepDirection = Direction.directionOf(pivot, to);

        Coordinate occupiedFieldInDirection = occupiedFieldInDirection(chessBoard, deepDirection, pivot);
        if (occupiedFieldInDirection == null) return null;

        Piece opponentPiece = chessBoard.piece(occupiedFieldInDirection);
        if (opponentPiece.color() == color) return null;

        if (direction == SimpleDirection.VERTICAL || direction == SimpleDirection.HORIZONTAL) {
            if (!clearPath(chessBoard, pivot, to)) return null;
            if (opponentPiece instanceof Rook || opponentPiece instanceof Queen) return occupiedFieldInDirection;
            return null;
        }

        if (!clearPath(chessBoard, pivot, to)) return null;
        if (opponentPiece instanceof Bishop || opponentPiece instanceof Queen) return occupiedFieldInDirection;
        return null;
    }

    @Nullable
    private Coordinate checkEnemyInDirection(
            ChessBoard chessBoard,
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
            if (!clearPath(chessBoard, pivot, to)) return null;
            if (piece instanceof Rook || piece instanceof Queen) return to;
            return null;
        }

        if (isSurrounded) {
            if (piece instanceof Pawn) {
                if (color == WHITE && to.row() > pivot.row()) return to;
                if (color == BLACK && to.row() < pivot.row()) return to;
            }
            if (piece instanceof Bishop || piece instanceof Queen) return to;
            return null;
        }
        if (!clearPath(chessBoard, pivot, to)) return null;
        if (piece instanceof Bishop || piece instanceof Queen) return to;
        return null;
    }

    private boolean pawnDiagonalAttack(Coordinate pivot, Coordinate to) {
        if (color == WHITE) return to.row() > pivot.row();
        return to.row() < pivot.row();
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

        if (color == BLACK && square == 60) {
            moves |= 1L << 62;
            moves |= 1L << 58;
        }
        if (color == WHITE && square == 4) {
            moves |= 1L << 6;
            moves |= 1L << 2;
        }
        return moves;
    }

    @Override
    public String toString() {
        return "King{" +
                "color=" + color +
                ", index=" + index +
                '}';
    }
}