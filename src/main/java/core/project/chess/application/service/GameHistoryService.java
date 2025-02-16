package core.project.chess.application.service;

import core.project.chess.application.dto.chess.ChessGameHistory;
import core.project.chess.domain.chess.repositories.OutboundChessRepository;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Result;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class GameHistoryService {

    private final OutboundChessRepository outboundChessRepository;

    GameHistoryService(OutboundChessRepository outboundChessRepository) {
        this.outboundChessRepository = outboundChessRepository;
    }

    public ChessGameHistory getGameByID(String gameID) {
        UUID chessGameId = Result.ofThrowable(() -> UUID.fromString(gameID))
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid gameID").build()));

        return outboundChessRepository
                .findById(chessGameId)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Can`t find a game history.").build())
                );
    }

    public List<ChessGameHistory> listOfGames(String name, int pageNumber, int pageSize) {
        Username username = Result.ofThrowable(() -> new Username(name))
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid username.").build()));

        int limit = buildLimit(pageSize);
        int offSet = buildOffSet(limit, pageNumber);
        return outboundChessRepository
                .listOfGames(username, limit, offSet)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("User does not exist.\uD83D\uDC7B").build())
                );
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
