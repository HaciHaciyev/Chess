package core.project.chess.infrastructure.dal.cache;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.entities.Puzzle;
import core.project.chess.domain.commons.tuples.Pair;
import core.project.chess.domain.commons.value_objects.GameRequest;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.value_objects.Username;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ApplicationScoped
public class SessionStorage {

    /**
    * Used to store users and associated sessions
    * single user can have only one associated session
    * view:
    * username -> (Session, UserAccount)
    */
    private static final ConcurrentHashMap<Username, Pair<Session, User>> sessions = new ConcurrentHashMap<>();
    
    /**
    * Used to store users and associated puzzles
    * single user can have multiple puzzles associated
    * view:
    * (username, puzzle_id) -> Puzzle
    */
    private static final ConcurrentHashMap<Pair<Username, UUID>, Puzzle> puzzles = new ConcurrentHashMap<>();
    
    /**
    * Used to store games and associated sessions
    * single game can be associated with many sessions
    * view:
    * game id -> (game, list of sessions)
    */
    private static final ConcurrentHashMap<UUID, Pair<ChessGame, CopyOnWriteArraySet<Session>>> gameSessions = new ConcurrentHashMap<>();
    
    /**
    * Used to store users who are waiting for opponents to be found
    * single user can be waiting on multiple games
    */
    private static final ConcurrentHashMap<Username, ConcurrentLinkedDeque<GameRequest>> waitingForTheGame = new ConcurrentHashMap<>();
    
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void addWaitingUser(GameRequest gameRequest) {
        Username username = new Username(gameRequest.user().username());
        
        waitingForTheGame
                .computeIfAbsent(username, k -> new ConcurrentLinkedDeque<>())
                .offerLast(gameRequest);
    }
    
    public void removeLastGameSearchRequestOf(Username username) {
        var computedQueue = waitingForTheGame.computeIfPresent(username, (key, queue) -> {
            queue.pollLast();
            return queue;
        });

        if (computedQueue == null || computedQueue.isEmpty()) waitingForTheGame.remove(username);
    }

    public boolean removeWaitingUser(GameRequest waitingUser) {
        Username username = new Username(waitingUser.user().username());
        var queue = waitingForTheGame.get(username);
        if (queue == null) return false;
        
        final boolean removed = queue.remove(waitingUser);
        if (removed) {
            if (queue.isEmpty()) waitingForTheGame.remove(username);
            return true;
        }
        else return false;
    }

    public Set<Map.Entry<Username, ConcurrentLinkedDeque<GameRequest>>> waitingUsers() {
        return Collections.unmodifiableSet(waitingForTheGame.entrySet());
    }

    public Optional<Pair<Session, User>> getSessionByUsername(Username username) {
        return Optional.ofNullable(sessions.get(username));
    }

    public void addSession(Session session, User account) {
        sessions.put(new Username(account.username()), Pair.of(session, account));
    }

    public boolean containsSession(Username username) {
        return sessions.containsKey(username);
    }

    public void removeSession(Session session) {
        synchronized (session) {
            sessions.entrySet().removeIf(entry -> entry.getValue().getFirst().equals(session));
        }
    }

    public void removeSession(Username username) {
        lock.writeLock().lock();
        try {
            sessions.remove(username);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<ChessGame> getGameById(UUID gameId) {
        Pair<ChessGame, CopyOnWriteArraySet<Session>> pair = gameSessions.get(gameId);
        return (pair == null) ? Optional.empty() : Optional.of(pair.getFirst());
    }

    public void addGame(ChessGame game, HashSet<Session> sessions) {
        gameSessions.put(game.chessGameID(), Pair.of(game, new CopyOnWriteArraySet<>(sessions)));
    }

    public void addSessionToGame(UUID gameId, Session session) {
        gameSessions.computeIfPresent(gameId, (id, pair) -> {
            pair.getSecond().add(session);
            return pair;
        });
    }

    public Set<Session> getGameSessions(UUID gameId) {
        Pair<ChessGame, CopyOnWriteArraySet<Session>> gameData = gameSessions.get(gameId);
        return (gameData == null) ? Collections.emptySet() : gameData.getSecond();
    }

    public boolean containsGame(UUID gameUuid) {
        return gameSessions.containsKey(gameUuid);
    }

    public Optional<ChessGame> removeGame(UUID gameId) {
        Pair<ChessGame, CopyOnWriteArraySet<Session>> pair = gameSessions.remove(gameId);
        return (pair == null) ? Optional.empty() : Optional.of(pair.getFirst());
    }

    public void addPuzzle(Puzzle puzzle) {
        puzzles.put(Pair.of(new Username(puzzle.player().username()), puzzle.ID()), puzzle);
    }

    public Optional<Puzzle> getPuzzle(Username username, UUID puzzleID) {
        return Optional.ofNullable(puzzles.get(Pair.of(username, puzzleID)));
    }
}