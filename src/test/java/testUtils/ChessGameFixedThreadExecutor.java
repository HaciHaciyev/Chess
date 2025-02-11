package testUtils;

import io.quarkus.logging.Log;
import org.opentest4j.AssertionFailedError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static core.project.chess.domain.entities.ChessGameTest.executeGameFromPGN;

/**
 * A utility class for executing chess games from PGN files using a fixed thread pool.
 * This executor partitions the PGN data and processes it concurrently for improved performance.
 */

public final class ChessGameFixedThreadExecutor {

    private final String path;
    private final ExecutorService executorService;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final AtomicInteger totalGameExecutions = new AtomicInteger();
    private final AtomicInteger totalGameFailures = new AtomicInteger();

    private final boolean enableGameLogging;
    private final boolean enableExecutorLogging;
    private final boolean enableAssertions;

    private final int numThreads;
    private final int numPartitions;

    private static final int TIMEOUT_MINUTES = 15;

    private final BlockingQueue<Partition> queue = new LinkedBlockingQueue<>();
    private final ConcurrentLinkedQueue<String> gameFailures = new ConcurrentLinkedQueue<>();

    /**
     * Constructs a new ChessGameFixedThreadExecutor.
     *
     * @param path                  The path to the PGN file
     * @param numThreads            The number of consumer threads to use
     * @param numPartitions         The number of partitions to divide the PGN data into
     * @param enableExecutorLogging Whether to enable logging for the executor
     * @param enableGameLogging     Whether to enable logging for individual games
     * @param enableAssertions      Whether to enable assertions during game execution
     */
    public ChessGameFixedThreadExecutor(String path,
                                        int numThreads,
                                        int numPartitions,
                                        boolean enableExecutorLogging,
                                        boolean enableGameLogging,
                                        boolean enableAssertions) {
        this.path = path;
        this.numThreads = numThreads;
        this.numPartitions = numPartitions;
        this.enableGameLogging = enableGameLogging;
        this.enableExecutorLogging = enableExecutorLogging;
        this.enableAssertions = enableAssertions;

        // Create a fixed thread pool with custom thread naming
        AtomicInteger counter = new AtomicInteger(1);
        this.executorService = Executors.newFixedThreadPool(numThreads + 1, r -> {
            if (counter.get() == 1) {
                counter.getAndIncrement();
                return new Thread(r, "producer");
            }
            return new Thread(r, "consumer-" + counter.getAndIncrement());
        });
    }

    /**
     * Starts the execution of chess games.
     *
     * @return true if the executor was started successfully, false if it was already running
     */
    public boolean start() {
        if (!isRunning.compareAndSet(false, true)) {
            Log.info("Executor is already running");
            throw new IllegalStateException("Eee");
        }

        Log.info("Launching the executor");

        // Start the producer thread
        executorService.execute(this::produce);

        // Start the consumer threads
        for (int i = 0; i < numThreads; i++) {
            executorService.execute(this::consume);
        }

        shutdownAndAwaitTermination();

        Log.info("Total game executions: " + totalGameExecutions);
        Log.info("Failures: " + totalGameFailures);
        gameFailures.forEach(Log::error);

        return gameFailures.isEmpty();
    }

    /**
     * Producer method that reads the PGN file, partitions the data, and adds it to the queue.
     */
    private void produce() {
        try {
            log("Extracting from " + path.substring(path.lastIndexOf("/") + 1));

            List<String> pgnList = SimplePGNReader.extractFromPGN(path);

            log("Extracted " + pgnList.size() + " pgns");
            log("Number of partitions: " + numPartitions);

            int partitionSize = (int) Math.ceil((double) pgnList.size() / numPartitions);

            log("Calculated partition size: " + partitionSize);

            List<Partition> partitions = partitionBasedOnSize(pgnList, partitionSize);

            // Add partitions to the queue
            for (Partition partition : partitions) {
                queue.put(partition);
                log("Put partition#%d with Size#%d into queue".formatted(partition.num(), partition.pgnList().size()));
            }

            log("All partitions have been delivered");
        } catch (Exception e) {
            Log.error("Error in producer", e);
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * Consumer method that processes partitions from the queue and executes chess games.
     */
    private void consume() {
        int partitionExecutions = 0;
        int gameExecutions = 0;

        while (isRunning.get() || !queue.isEmpty()) {
            try {
                Partition partition = queue.poll(100, TimeUnit.MILLISECONDS);

                if (partition != null) {
                    log("Processing partition#" + partition.num());
                    gameExecutions += executeGames(partition);
                    log("Processed partition#" + partition.num());
                    log("Processed %s games".formatted(gameExecutions));
                    partitionExecutions++;
                }

                if (!isRunning.get() && queue.isEmpty()) {
                    log("Processed %d games in %d partitions".formatted(gameExecutions, partitionExecutions));
                    break;
                }

            } catch (InterruptedException e) {
                Log.error("Consumer interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.error("Error in consumer", e);
            }
        }
    }

    /**
     * Partitions the PGN list into smaller sublists based on the specified partition size.
     *
     * @param pgnList The list of PGN strings to partition
     * @param partSize The size of each partition
     * @return A list of Partition objects
     */
    private List<Partition> partitionBasedOnSize(List<String> pgnList, int partSize) {
        List<Partition> parts = new ArrayList<>();
        final int pgnListSize = pgnList.size();
        int num = 1;

        for (int i = 0; i < pgnListSize; i += partSize) {
            List<String> pgnSubList = pgnList.subList(i, Math.min(pgnListSize, i + partSize));
            parts.add(new Partition(pgnSubList, num++, partSize));
        }
        return parts;
    }

    /**
     * Executes chess games for a given partition.
     *
     * @param partition The partition containing PGN strings to execute
     */
    private int executeGames(Partition partition) {
        int gameNum = (partition.num() - 1) * partition.size();
        int gameExecutions = 0;
        for (String pgn : partition.pgnList()) {
            gameExecutions++;
            totalGameExecutions.incrementAndGet();

            try {
                executeGameFromPGN(pgn, gameNum++, enableGameLogging, enableAssertions, false);
            } catch (AssertionFailedError | IllegalStateException e) {
                totalGameFailures.incrementAndGet();
                gameFailures.offer(e.getMessage());
            }
        }

        return gameExecutions;
    }

    /**
     * Shuts down the executor service and waits for termination.
     */
    private void shutdownAndAwaitTermination() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                    Log.error("ExecutorService did not terminate");
                }
            }

            Log.info("Executor is shut down");
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Logs a message if executor logging is enabled.
     *
     * @param str The message to log
     */
    private void log(String str) {
        if (enableExecutorLogging) {
            Log.info(str);
        }
    }

    /**
     * Represents a partition of PGN data to be processed.
     *
     * @param pgnList The list of PGN strings in this partition
     * @param num The partition number
     * @param size The size of the partition
     */
    private record Partition(List<String> pgnList, int num, int size) {}
}