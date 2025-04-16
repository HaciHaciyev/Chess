package core.project.chess.infrastructure.dal.cache;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.entities.Puzzle;
import core.project.chess.domain.chess.value_objects.GameParameters;
import core.project.chess.domain.commons.tuples.Pair;
import core.project.chess.domain.commons.tuples.Triple;
import core.project.chess.domain.user.entities.UserAccount;
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

    private static final ConcurrentHashMap<String, Pair<Session, UserAccount>> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Pair<String, UUID>, Puzzle> puzzles = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Pair<ChessGame, CopyOnWriteArraySet<Session>>> gameSessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentLinkedDeque<Triple<Session, UserAccount, GameParameters>>> waitingForTheGame = new ConcurrentHashMap<>();
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void addWaitingUser(Session session, UserAccount account, GameParameters gameParameters) {
        waitingForTheGame
                .computeIfAbsent(account.getUsername(), k -> new ConcurrentLinkedDeque<>())
                .offerLast(Triple.of(session, account, gameParameters));
    }

    public Optional<Triple<Session, UserAccount, GameParameters>> getWaitingUser(String username) {
        var queue = waitingForTheGame.get(username);
        return (queue == null) ? Optional.empty() : Optional.of(queue.peekLast());
    }

    public void removeWaitingUser(String username) {
        waitingForTheGame.computeIfPresent(username, (key, queue) -> {
            queue.pollLast();
            return queue.isEmpty() ? null : queue;
        });
    }

    public Set<Map.Entry<String, ConcurrentLinkedDeque<Triple<Session, UserAccount, GameParameters>>>> waitingUsers() {
        return waitingForTheGame.entrySet();
    }

    public Optional<Pair<Session, UserAccount>> getSessionByUsername(String username) {
        return Optional.ofNullable(sessions.get(username));
    }

    public void addSession(Session session, UserAccount account) {
        sessions.put(account.getUsername(), Pair.of(session, account));
    }

    public boolean containsSession(String addresseeUsername) {
        return sessions.containsKey(addresseeUsername);
    }

    public void removeSession(Session session) {
        lock.writeLock().lock();
        try {
            sessions.entrySet().removeIf(entry -> entry.getValue().getFirst().equals(session));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<ChessGame> getGameById(UUID gameId) {
        Pair<ChessGame, CopyOnWriteArraySet<Session>> pair = gameSessions.get(gameId);
        return (pair == null) ? Optional.empty() : Optional.of(pair.getFirst());
    }

    public void addGame(ChessGame game, HashSet<Session> sessions) {
        gameSessions.put(game.getChessGameId(), Pair.of(game, new CopyOnWriteArraySet<>(sessions)));
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
        puzzles.put(Pair.of(puzzle.player().getUsername(), puzzle.ID()), puzzle);
    }

    public Optional<Puzzle> getPuzzle(String username, UUID puzzleID) {
        return Optional.ofNullable(puzzles.get(Pair.of(username, puzzleID)));
    }
}