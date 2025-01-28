package core.project.chess.infrastructure.dal.cache;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.value_objects.GameParameters;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Triple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SessionStorage {

    private static final ConcurrentHashMap<Username, Pair<Session, UserAccount>> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Pair<ChessGame, HashSet<Session>>> gameSessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Username, Triple<Session, UserAccount, GameParameters>> waitingForTheGame = new ConcurrentHashMap<>();

    public Pair<Session, UserAccount> getSessionByUsername(Username username) {
        return sessions.get(username);
    }

    public void addSession(Session session, UserAccount account) {
        sessions.put(account.getUsername(), Pair.of(session, account));
    }

    public boolean containsSession(Username addresseeUsername) {
        return sessions.containsKey(addresseeUsername);
    }

    public void removeSession(Session session) {
        sessions.entrySet().removeIf(entry -> entry.getValue().getFirst().equals(session));
    }


    public Pair<ChessGame, HashSet<Session>> getGameById(UUID gameId) {
        return gameSessions.get(gameId);
    }

    public void addGame(ChessGame game, HashSet<Session> sessions) {
        gameSessions.put(game.getChessGameId(), Pair.of(game, sessions));
    }

    public boolean containsGame(UUID gameUuid) {
        return gameSessions.containsKey(gameUuid);
    }

    public Pair<ChessGame, HashSet<Session>> removeGame(UUID gameUuid) {
        return gameSessions.remove(gameUuid);
    }

    public Triple<Session, UserAccount, GameParameters> getWaitingUser(Username username) {
        return waitingForTheGame.get(username);
    }

    public void addWaitingUser(Session session, UserAccount account, GameParameters gameParameters) {
        waitingForTheGame.put(account.getUsername(), Triple.of(session, account, gameParameters));
    }

    public void removeWaitingUser(Username username) {
        waitingForTheGame.remove(username);
    }

    public Set<Map.Entry<Username, Triple<Session, UserAccount, GameParameters>>> waitingUsers() {
        return waitingForTheGame.entrySet();
    }
}
