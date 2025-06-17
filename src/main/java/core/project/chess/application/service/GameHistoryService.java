package core.project.chess.application.service;

import core.project.chess.application.dto.chess.ChessGameHistory;
import core.project.chess.domain.chess.repositories.OutboundChessRepository;
import core.project.chess.domain.commons.value_objects.Username;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

import static core.project.chess.application.util.JSONUtilities.responseException;

@ApplicationScoped
public class GameHistoryService {

    private final OutboundChessRepository outboundChessRepository;

    GameHistoryService(OutboundChessRepository outboundChessRepository) {
        this.outboundChessRepository = outboundChessRepository;
    }

    public ChessGameHistory getGameByID(String gameID) {
        UUID chessGameId;
        try {
            chessGameId = UUID.fromString(gameID);
        } catch (IllegalArgumentException e) {
            throw responseException(Response.Status.BAD_REQUEST, "Invalid gameID.");
        }

        return outboundChessRepository.findById(chessGameId)
                .orElseThrow(() -> responseException(Response.Status.NOT_FOUND, "Can`t find chess game history."));
    }

    public List<ChessGameHistory> listOfGames(String username, int pageNumber, int pageSize) {
        Username usernameObj;
        try {
            usernameObj = new Username(username);
        } catch (IllegalArgumentException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }

        int limit = buildLimit(pageSize);
        int offSet = buildOffSet(limit, pageNumber);
        return outboundChessRepository
                .listOfGames(usernameObj, limit, offSet)
                .orElseThrow(() -> responseException(Response.Status.NOT_FOUND, "User does not exist.\uD83D\uDC7B"));
    }

    static int buildLimit(Integer pageSize) {
        int limit;
        if (pageSize > 0 && pageSize <= 25) {
            limit = pageSize;
        } else {
            limit = 10;
        }
        return limit;
    }

    static int buildOffSet(Integer limit, Integer pageNumber) {
        int offSet;
        if (limit > 0 && pageNumber > 0) {
            offSet = (pageNumber - 1) * limit;
        } else {
            offSet = 0;
        }
        return offSet;
    }
}
