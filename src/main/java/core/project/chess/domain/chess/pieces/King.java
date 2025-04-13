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

            Operations operation = isSurrounded(surroundingFields(kingCoordinate), boardNavigator, kingCoordinate) ?
                    Operations.CHECKMATE :
                    Operations.CHECK;
            return new KingStatus(operation, enemies);
        }

        if (kingMove(boardNavigator.board(), kingCoordinate, enemies.getFirst()) &&
                !isFieldDangerousOrBlocked(boardNavigator, enemies.getFirst(), kingCoordinate))
            return new KingStatus(Operations.CHECK, enemies);

        if (kingMove(boardNavigator.board(), kingCoordinate, enemies.getLast()) &&
                !isFieldDangerousOrBlocked(boardNavigator, enemies.getLast(), kingCoordinate))
            return new KingStatus(Operations.CHECK, enemies);

        Operations operation = isSurrounded(surroundingFields(kingCoordinate), boardNavigator, kingCoordinate) ?
                Operations.CHECKMATE :
                Operations.CHECK;
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

    private boolean safeToCastle(ChessBoardNavigator navigator, Coordinate presentKingPosition, Coordinate futureKingPosition) {
        Castle castle;
        if (presentKingPosition.column() < futureKingPosition.column()) castle = Castle.SHORT_CASTLING;
        else castle = Castle.LONG_CASTLING;

        KingStatus kingStatus = navigator.board().kingStatus();
        List<Coordinate> attackers = kingStatus != null ? kingStatus.enemiesAttackingTheKing() : check(navigator, null);
        if (!attackers.isEmpty()) return false;

        List<Coordinate> fieldsToCastle = castlingFields(castle, color);
        for (int i = 1; i < fieldsToCastle.size(); i++) {
            Coordinate field = fieldsToCastle.get(i);
            final boolean pathIsNotClear = navigator.board().piece(field) != null;
            if (pathIsNotClear) return false;
            if (isFieldDangerousOrBlocked(navigator, field, presentKingPosition)) return false;
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

    private boolean canEat(ChessBoardNavigator navigator, Coordinate target) {
        ChessBoard board = navigator.board();
        int square = target.ordinal();
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

    private boolean canBlock(ChessBoardNavigator navigator, Coordinate pivot, Coordinate enemyField) {
        Piece opponentPiece = navigator.board().piece(enemyField);
        if (opponentPiece instanceof Knight) return false;

        final boolean surround = Math.abs(pivot.row() - enemyField.row()) <= 1 &&
                Math.abs(pivot.column() - enemyField.column()) <= 1;
        if (surround) return false;

        final boolean vertical = pivot.column() == enemyField.column() && pivot.row() != enemyField.row();
        for (Coordinate target : fieldsInPath(pivot, enemyField)) {
            if (!vertical && pawnCanBlock(navigator, target)) return true;

            ChessBoard board = navigator.board();
            int square = target.ordinal();
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
            Coordinate from = Coordinate.byOrdinal(fromMask);
            if (safeForKing(board, from, target)) return true;
            pawnAttackers &= pawnAttackers - 1;
        }
        return false;
    }

    private boolean enPassant(Coordinate target, ChessBoard board, long[][] table, long ourPawns) {
        Coordinate enPassant = board.enPassant();
        if (enPassant != null && board.piece(target) instanceof Pawn) {
            if (validateEnPassantAbility(target, enPassant)) return false;
            long enPassantAttackersMask = table[Checkers.PAWNS.ordinal()][enPassant.ordinal()] & ourPawns;
            while (enPassantAttackersMask != 0) {
                int fromMask = Long.numberOfTrailingZeros(enPassantAttackersMask);
                if (safeForKing(board, Coordinate.byOrdinal(fromMask), enPassant)) return true;
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
            if (safeForKing(board, Coordinate.byOrdinal(fromMask), target)) return true;
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
            Coordinate from = Coordinate.byOrdinal(fromMask);
            if (clearPath(board, from, target) &&
                    safeForKing(board, from, target)) return true;
            diagonalAttackers &= diagonalAttackers - 1;
        }

        long orthogonalAttackers = table[Checkers.ORTHOGONALS.ordinal()][square] & ourRooksQueens;
        while (orthogonalAttackers != 0) {
            int fromMask = Long.numberOfTrailingZeros(orthogonalAttackers);
            Coordinate from = Coordinate.byOrdinal(fromMask);
            if (clearPath(board, from, target) &&
                    safeForKing(board, from, target)) return true;
            orthogonalAttackers &= orthogonalAttackers - 1;
        }
        return false;
    }

    private boolean pawnCanBlock(ChessBoardNavigator boardNavigator, Coordinate field) {
        Coordinate canBlockByOnePush;
        if (color == WHITE) canBlockByOnePush = Coordinate.of(field.row() - 1, field.column());
        else canBlockByOnePush = Coordinate.of(field.row() + 1, field.column());
        if (canBlockByOnePush != null) {
            Piece possiblePawn = boardNavigator.board().piece(canBlockByOnePush);
            if (possiblePawn != null) {
                final boolean pawnCanBlock = possiblePawn.color() == color &&
                        possiblePawn instanceof Pawn &&
                        safeForKing(boardNavigator.board(), canBlockByOnePush, field);
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