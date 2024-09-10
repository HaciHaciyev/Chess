package testUtils;

import core.project.chess.infrastructure.utilities.chess.SimplePGNReader;
import io.quarkus.logging.Log;
import org.opentest4j.AssertionFailedError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static core.project.chess.domain.aggregates.chess.entities.ChessGameTest.executeGameFromPGN;

public final class ChessGameFixedThreadExecutor {

    private final Producer producer;
    private final Consumer consumer;

    private final ExecutorService producerService;
    private final ExecutorService consumerService;

    private final AtomicBoolean runningProducer = new AtomicBoolean(false);
    private final AtomicBoolean runningConsumer = new AtomicBoolean(false);

    private final AtomicInteger totalGameExecutions = new AtomicInteger();
    private final AtomicInteger totalGameFailures = new AtomicInteger();

    private final boolean enableGameLogging;
    private final boolean enableExecutorLogging;
    private final boolean enableAssertions;

    private final int NUM_CONSUMERS;
    private final int NUM_PARTITIONS;

    private static final int TIMEOUT_S = 15;

    private final BlockingQueue<Partition> queue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<String, Integer> gameFailures = new ConcurrentHashMap<>();

    public ChessGameFixedThreadExecutor(String path,
                                        int consumers,
                                        int partitions,
                                        boolean enableExecutorLogging,
                                        boolean enableGameLogging,
                                        boolean enableAssertions) {
        this.producer = new Producer(path);
        this.consumer = new Consumer();
        this.NUM_CONSUMERS = consumers;
        this.NUM_PARTITIONS = partitions;
        this.enableGameLogging = enableGameLogging;
        this.enableExecutorLogging = enableExecutorLogging;
        this.enableAssertions = enableAssertions;


        this.producerService = Executors.newSingleThreadExecutor(r -> new Thread(r, "producer"));
        AtomicInteger counter = new AtomicInteger();
        this.consumerService = Executors.newFixedThreadPool(NUM_CONSUMERS, r -> new Thread(r, "consumer#" + counter.incrementAndGet()));
    }

    public boolean start() {
        if (!runningProducer.compareAndSet(false, true) || !runningConsumer.compareAndSet(false, true)) {
            Log.info("Executor is already running");
            return false;
        }

        Log.info("Launching the executor");

        producerService.execute(producer);

        for (int i = 0; i < NUM_CONSUMERS; i++) {
            consumerService.execute(consumer);
        }

        while (true) {
            if (!runningProducer.get() && !producerService.isTerminated()) {
                shutdownProducer();
            }

            if (!runningConsumer.get()) {
                shutdownConsumer();
                break;
            }
        }

        Log.info("Total game executions: " + totalGameExecutions);
        Log.info("Failures: " + totalGameFailures);
        gameFailures.forEachKey(1, Log::info);

        return true;
    }

    private void shutdownConsumer() {
        boolean success = shutdownExecutor(consumerService);

        if (success && enableExecutorLogging && consumerService.isTerminated()) {
            Log.info("Consumer shutdown complete");
        }
    }

    private void shutdownProducer() {
        boolean success = shutdownExecutor(producerService);

        if (success && enableExecutorLogging && producerService.isTerminated()) {
            Log.info("Producer shutdown complete");
        }
    }

    private boolean shutdownExecutor(ExecutorService executor) {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(TIMEOUT_S, TimeUnit.SECONDS)) {
                executor.shutdownNow();

                return executor.awaitTermination(TIMEOUT_S, TimeUnit.SECONDS);
            }

            return true;
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            Log.warn(ex);
        }

        return false;
    }

    public void log(String str) {
        if (enableExecutorLogging) {
            Log.info(str);
        }
    }

    private record Partition(List<String> pgnList, int num, int size) {
    }

    private class Producer implements Runnable {

        String path;

        public Producer(String path) {
            this.path = path;
        }

        @Override
        public void run() {
            try {
                log("Extracting from " + path.substring(path.lastIndexOf("/") + 1));

                List<String> pgnList = SimplePGNReader.extractFromPGN(path);

                log("Extracted " + pgnList.size() + " pgns");
                log("Number of partitions: " + NUM_PARTITIONS);

                int partitionSize = (int) Math.ceil((double) pgnList.size() / NUM_PARTITIONS);

                log("Calculated partition size: " + partitionSize);

                List<Partition> partitions = partitionBasedOnSize(pgnList, partitionSize);

                for (Partition partition : partitions) {
                    log("Partition#%d | Size#%d".formatted(partition.num, partition.pgnList.size()));
                }

                for (Partition partition : partitions) {
                    queue.put(partition);
                    log("Put partition#%d into queue".formatted(partition.num));
                }

                log("All partitions have been delivered, shutting down...");
            } catch (Exception e) {
                Log.error("Error in producer", e);
            } finally {
                runningProducer.set(false);
            }
        }

        private List<Partition> partitionBasedOnSize(List<String> pgnList, int partSize) {
            List<Partition> parts = new ArrayList<>();
            final int pgnListSize = pgnList.size();
            int num = 1;

            for (int i = 0; i < pgnListSize; i += partSize) {
                ArrayList<String> pgnSubList = new ArrayList<>(pgnList.subList(i, Math.min(pgnListSize, i + partSize)));

                Partition partition = new Partition(
                        pgnSubList,
                        num++,
                        partSize
                );

                parts.add(partition);
            }
            return parts;
        }
    }

    private class Consumer implements Runnable {

        @Override
        public void run() {
            int partitionExecutions = 0;
            AtomicInteger gameExecutions = new AtomicInteger(0);

            while (runningConsumer.get() || !queue.isEmpty()) {
                try {
                    Partition partition = queue.poll(100, TimeUnit.MILLISECONDS);

                    if (partition != null) {
                        consume(partition, gameExecutions, totalGameFailures, enableGameLogging, enableAssertions);
                        log("Processed partition#" + partition.num);
                        partitionExecutions++;
                    }

                    if (!runningProducer.get() && queue.isEmpty()) {
                        log("Processed %d games in %d partitions, shutting down...".formatted(gameExecutions.get(), partitionExecutions));
                        runningConsumer.set(false);
                    }

                } catch (InterruptedException e) {
                    Log.error("Consumer interrupted", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Log.error("Error in consumer", e);
                }
            }
        }

        void consume(Partition partition,
                     AtomicInteger gameExecutions,
                     AtomicInteger failures,
                     boolean enableLogging,
                     boolean enableAssertions) {

            int gameNum = (partition.num - 1) * partition.size;

            for (String pgn : partition.pgnList) {
                gameExecutions.incrementAndGet();
                totalGameExecutions.incrementAndGet();

                try {
                    executeGameFromPGN(pgn, gameNum++, enableLogging, enableAssertions);
                } catch (AssertionFailedError | IllegalStateException e) {
                    failures.incrementAndGet();

                    if (gameFailures.containsKey(e.getMessage())) {
                        gameFailures.computeIfPresent(e.getMessage(), (k, v) -> v + 1);
                    } else {
                        gameFailures.put(e.getMessage(), 1);
                    }
                }
            }
        }
    }
}