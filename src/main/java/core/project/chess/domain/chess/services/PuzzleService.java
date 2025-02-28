package core.project.chess.domain.chess.services;

import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.domain.chess.entities.Puzzle;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.repositories.OutboundChessRepository;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.infrastructure.dal.cache.SessionStorage;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PuzzleService {

    private final SessionStorage sessionStorage;

    private final OutboundChessRepository outboundChessRepository;

    PuzzleService(SessionStorage sessionStorage, OutboundChessRepository outboundChessRepository) {
        this.sessionStorage = sessionStorage;
        this.outboundChessRepository = outboundChessRepository;
    }

    public Message chessPuzzle(UserAccount user) {
        var puzzleProperties = outboundChessRepository.puzzle(user.getRating().rating()).orElseThrow();
        Puzzle puzzle = Puzzle.of(user, puzzleProperties.PGN(), puzzleProperties.startPosition(), puzzleProperties.rating());

        sessionStorage.addPuzzle(puzzle);

        return Message.builder(MessageType.PUZZLE)
                .gameID(puzzle.ID().toString())
                .FEN(puzzle.chessBoard().actualRepresentationOfChessBoard())
                .PGN(puzzle.chessBoard().pgn())
                .isPuzzleEnded(puzzle.isEnded())
                .isPuzzleSolved(puzzle.isSolved())
                .build();
    }

    public Message puzzleMove(UserAccount user, UUID puzzleID, Coordinate from, Coordinate to, @Nullable String promotion) {
        Optional<Puzzle> puzzleOptional = sessionStorage.getPuzzle(user.getUsername(), puzzleID);
        if (puzzleOptional.isEmpty()) {
            return Message.error("This puzzle session do not exists.");
        }

        try {
            Piece inCaseOfPromotion = Objects.isNull(promotion) ? null : AlgebraicNotation.fromSymbol(promotion);

            Puzzle puzzle = puzzleOptional.get();
            puzzle.makeMovement(from, to, inCaseOfPromotion);

            return Message.builder(MessageType.PUZZLE_MOVE)
                    .gameID(puzzle.ID().toString())
                    .FEN(puzzle.chessBoard().actualRepresentationOfChessBoard())
                    .PGN(puzzle.chessBoard().pgn())
                    .build();
        } catch (IllegalArgumentException e) {
            return Message.error("Invalid move.");
        }
    }
}
