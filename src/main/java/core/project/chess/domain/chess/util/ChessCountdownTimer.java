package core.project.chess.domain.chess.util;

import core.project.chess.domain.chess.entities.ChessGame;
import io.quarkus.logging.Log;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChessCountdownTimer implements Runnable {

    private final ChessGame chessGame;
    private Instant startTime;
    private Instant pauseTime;
    private final Duration gameDuration;

    private final AtomicBoolean isPaused;
    private final AtomicBoolean isRunning;

    private final Object lock;
    private final Thread timerThread;

    private final Runnable onComplete;

    private final String name;

    private static int threadID = 0;

    public ChessCountdownTimer(ChessGame chessGame, String name, Duration duration, Runnable onComplete) {
        this.chessGame = chessGame;
        this.gameDuration = duration;
        this.isPaused = new AtomicBoolean();
        this.isRunning = new AtomicBoolean();
        this.lock = new Object();
        this.timerThread = Thread.ofVirtual().unstarted(this);
        this.onComplete = onComplete;

        this.name = name + "#" + threadID++;
    }

    public String getName() {
        return name;
    }

    @Override
    public void run() {
        try {
            while (isRunning.get()) {
                synchronized (lock) {
                    if (isPaused.get()) {
                        lock.wait();
                    }
                }

                Duration remaining = remainingTime();

                if (remaining.isNegative() || remaining.isZero()) {
                    onComplete.run();
                    stop();
                    break;
                }

                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    public Duration remainingTime() {
        Duration elapsed = Duration.between(startTime, Instant.now());
        return gameDuration.minus(elapsed);
    }

    public void start() {
        if (isRunning.get() && !isPaused.get()) {
            Log.warnf("%s for the game %s is already running", name, chessGame.getChessGameId());
            return;
        }


        isRunning.set(true);

        if (isPaused.get()) {
            Duration pauseDuration = Duration.between(pauseTime, Instant.now());
            startTime = startTime.plus(pauseDuration);
            isPaused.set(false);

            synchronized (lock) {
                lock.notifyAll();
            }

            return;
        } else {
            startTime = Instant.now();
        }

        timerThread.start();
    }

    public void pause() {
        if (isRunning.get() && !isPaused.get()) {
            pauseTime = Instant.now();
            isPaused.set(true);
        }
    }

    public void stop() {
        isRunning.set(false);
        timerThread.interrupt();
    }
}
