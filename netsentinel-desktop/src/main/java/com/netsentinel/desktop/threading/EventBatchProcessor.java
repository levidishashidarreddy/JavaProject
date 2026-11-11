package com.netsentinel.desktop.threading;

import com.netsentinel.desktop.model.DesktopActivityLog;
import javafx.application.Platform;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * =========================================================================
 * NETSENTINEL MULTITHREADING DEMONSTRATION ENGINE
 * =========================================================================
 * 
 * Demonstrates Core Java Syllabus Multithreading Concepts:
 * 1. Thread creation via Runnable interface & Thread class
 * 2. Thread Pools with ExecutorService (newFixedThreadPool)
 * 3. Thread Lifecycle management (start, run, interrupt, shutdown, awaitTermination)
 * 4. Inter-Thread Communication via BlockingQueue (Producer-Consumer pattern)
 * 5. Explicit Thread Synchronization (synchronized block & intrinsic locks)
 * 6. Thread-safe collections (LinkedBlockingQueue, ConcurrentHashMap, CopyOnWriteArrayList)
 * 7. Non-blocking UI updates via JavaFX Platform.runLater()
 */
public class EventBatchProcessor {

    // Thread-safe communication queue between Producer and Consumer threads
    private final BlockingQueue<DesktopActivityLog> eventQueue = new LinkedBlockingQueue<>(500);

    // Thread-safe collection tracking category velocity statistics across worker threads
    private final ConcurrentHashMap<String, AtomicInteger> categoryCounts = new ConcurrentHashMap<>();

    // Thread-safe listener registry using CopyOnWriteArrayList
    private final List<Consumer<BatchSummary>> summaryListeners = new CopyOnWriteArrayList<>();

    // ExecutorService managing thread lifecycle
    private ExecutorService threadPool;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    // Explicit lock object for synchronization demonstration
    private final Object statsLock = new Object();
    private int totalProcessed = 0;
    private int anomalyCount = 0;

    public static class BatchSummary {
        private final int totalItems;
        private final int anomalies;
        private final Map<String, Integer> breakdown;
        private final long durationMillis;

        public BatchSummary(int totalItems, int anomalies, Map<String, Integer> breakdown, long durationMillis) {
            this.totalItems = totalItems;
            this.anomalies = anomalies;
            this.breakdown = breakdown;
            this.durationMillis = durationMillis;
        }

        public int getTotalItems() { return totalItems; }
        public int getAnomalies() { return anomalies; }
        public Map<String, Integer> getBreakdown() { return breakdown; }
        public long getDurationMillis() { return durationMillis; }

        @Override
        public String toString() {
            return String.format("Processed %d events in %d ms (%d anomalies detected). Category distribution: %s",
                    totalItems, durationMillis, anomalies, breakdown);
        }
    }

    public EventBatchProcessor() {}

    /**
     * Start the background worker threads.
     * Demonstrates Thread creation, Runnable, and ExecutorService lifecycle.
     */
    public synchronized void start() {
        if (isRunning.get()) return;
        isRunning.set(true);

        // Create a fixed thread pool with 2 worker threads
        threadPool = Executors.newFixedThreadPool(2, new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "NetSentinel-Worker-" + (++counter));
                t.setDaemon(true); // Ensure threads don't block JVM exit
                return t;
            }
        });

        // Launch Consumer Worker Thread via Runnable
        Runnable consumerTask = this::runConsumerWorker;
        threadPool.submit(consumerTask);
    }

    /**
     * Producer method: Pushes a batch of activity events into the thread-safe queue.
     * Demonstrates Inter-Thread Communication.
     */
    public void enqueueBatch(List<DesktopActivityLog> events, Runnable onEnqueueDone) {
        if (threadPool == null || threadPool.isShutdown()) {
            start();
        }

        // Producer task runs on a dedicated thread to never freeze the JavaFX UI
        threadPool.submit(() -> {
            for (DesktopActivityLog ev : events) {
                try {
                    // BlockingQueue put() blocks if queue is full (Producer backpressure)
                    eventQueue.put(ev);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (onEnqueueDone != null) {
                runSafelyOnFxThread(onEnqueueDone);
            }
        });
    }

    private static void runSafelyOnFxThread(Runnable r) {
        try {
            Platform.runLater(r);
        } catch (IllegalStateException e) {
            // JavaFX Toolkit not initialized (e.g. running in headless unit tests)
            r.run();
        }
    }

    /**
     * Consumer Worker Thread Loop.
     * Demonstrates thread synchronization, wait/drain pattern, and safe JavaFX UI updates.
     */
    private void runConsumerWorker() {
        List<DesktopActivityLog> drainBuffer = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        while (isRunning.get() || !eventQueue.isEmpty()) {
            try {
                // Poll with timeout to prevent indefinite thread block
                DesktopActivityLog first = eventQueue.poll(500, TimeUnit.MILLISECONDS);
                if (first != null) {
                    drainBuffer.add(first);
                    // Drain up to 50 items for batch processing
                    eventQueue.drainTo(drainBuffer, 50);

                    // Demonstrates explicit synchronization block
                    synchronized (statsLock) {
                        for (DesktopActivityLog ev : drainBuffer) {
                            totalProcessed++;
                            String action = ev.getAction() != null ? ev.getAction() : "GENERIC_ACTION";
                            categoryCounts.computeIfAbsent(action, k -> new AtomicInteger(0)).incrementAndGet();

                            if ("DENIED".equalsIgnoreCase(ev.getOutcome()) || "BLOCKED".equalsIgnoreCase(ev.getOutcome())) {
                                anomalyCount++;
                            }
                        }
                    }

                    // Build summary map
                    Map<String, Integer> currentBreakdown = new HashMap<>();
                    categoryCounts.forEach((k, v) -> currentBreakdown.put(k, v.get()));

                    long elapsed = System.currentTimeMillis() - startTime;
                    BatchSummary summary = new BatchSummary(totalProcessed, anomalyCount, currentBreakdown, elapsed);

                    // Notify listeners safely on the JavaFX Application Thread
                    for (Consumer<BatchSummary> listener : summaryListeners) {
                        runSafelyOnFxThread(() -> listener.accept(summary));
                    }

                    drainBuffer.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void addSummaryListener(Consumer<BatchSummary> listener) {
        summaryListeners.add(listener);
    }

    /**
     * Graceful thread pool shutdown.
     * Demonstrates Thread Lifecycle termination and awaitTermination.
     */
    public synchronized void shutdown() {
        isRunning.set(false);
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(2, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public int getTotalProcessed() {
        synchronized (statsLock) {
            return totalProcessed;
        }
    }

    public int getAnomalyCount() {
        synchronized (statsLock) {
            return anomalyCount;
        }
    }
}

