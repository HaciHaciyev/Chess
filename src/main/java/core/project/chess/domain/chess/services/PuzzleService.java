package core.project.chess.domain.chess.services;

import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.domain.chess.entities.Puzzle;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.repositories.InboundChessRepository;
import core.project.chess.domain.chess.repositories.OutboundChessRepository;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.domain.user.entities.User;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
@ApplicationScoped
public class PuzzleService {

    private final OutboundChessRepository outboundChessRepository;

    private final InboundChessRepository inboundChessRepository;

    PuzzleService(OutboundChessRepository outboundChessRepository, InboundChessRepository inboundChessRepository) {
        this.outboundChessRepository = outboundChessRepository;
        this.inboundChessRepository = inboundChessRepository;
    }

    public Puzzle chessPuzzle(User user) {
        double minRating = user.puzzlesRating().rating() - Puzzle.USER_RATING_WINDOW;
        double maxRating = user.puzzlesRating().rating() + Puzzle.USER_RATING_WINDOW;
        var puzzleProperties = outboundChessRepository.puzzle(minRating, maxRating).orElseThrow();
        return Puzzle.fromRepository(
                puzzleProperties.puzzleId(),
                user, puzzleProperties.PGN(),
                puzzleProperties.startPosition(),
                puzzleProperties.rating()
        );
    }

    public Message puzzleMove(Puzzle puzzle, Coordinate from, Coordinate to, @Nullable String promotion) {
        try {
            Piece inCaseOfPromotion = Objects.isNull(promotion) ? null : AlgebraicNotation.fromSymbol(promotion);
            puzzle.makeMovement(from, to, inCaseOfPromotion);

            return Message.builder(MessageType.PUZZLE_MOVE)
                    .gameID(puzzle.ID().toString())
                    .FEN(puzzle.chessBoard().toString())
                    .PGN(puzzle.chessBoard().pgn())
                    .isPuzzleEnded(puzzle.isEnded())
                    .isPuzzleSolved(puzzle.isSolved())
                    .build();
        } catch (IllegalArgumentException e) {
            return Message.error("Invalid move.");
        }
    }

    public void save(String PGN, int startPositionOfPuzzle) {
        inboundChessRepository.savePuzzle(Puzzle.of(PGN, startPositionOfPuzzle));
    }
}
