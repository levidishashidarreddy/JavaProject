package com.netsentinel.desktop;

import com.netsentinel.desktop.collections.DataStore;
import com.netsentinel.desktop.model.DesktopActivityLog;
import com.netsentinel.desktop.model.DesktopIncident;
import com.netsentinel.desktop.syllabus.AwtAdapterDemo;
import com.netsentinel.desktop.syllabus.LegacyCollectionsDemo;
import com.netsentinel.desktop.threading.EventBatchProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying JavaFX Desktop Console models,
 * Multithreaded Batch Processor, Collections & Generics, and Syllabus Demos.
 */
public class DesktopConsoleTests {

    // =========================================================================
    // TEST 1: Collections & Generics — DataStore Operations
    // =========================================================================
    @Test
    @DisplayName("Collections & Generics: DataStore generic storage, indexing, and wildcards")
    void testDataStoreOperations() {
        DataStore<DesktopIncident> store = new DataStore<>(DesktopIncident::getIncidentNumber);

        DesktopIncident inc1 = new DesktopIncident();
        inc1.setId(1L);
        inc1.setIncidentNumber("INC-2026-0001");
        inc1.setTitle("Unauthorized Attendance Alteration");
        inc1.setSeverity("HIGH");

        DesktopIncident inc2 = new DesktopIncident();
        inc2.setId(2L);
        inc2.setIncidentNumber("INC-2026-0002");
        inc2.setTitle("Privilege Escalation Intercepted");
        inc2.setSeverity("CRITICAL");

        DesktopIncident inc3 = new DesktopIncident();
        inc3.setId(3L);
        inc3.setIncidentNumber("INC-2026-0003");
        inc3.setTitle("Normal Record View");
        inc3.setSeverity("LOW");

        store.addAll(List.of(inc1, inc2, inc3));

        assertEquals(3, store.size(), "Store should contain 3 items");
        assertTrue(store.containsKey("INC-2026-0001"));
        assertEquals("Privilege Escalation Intercepted", store.findByKey("INC-2026-0002").getTitle());

        // Test Generic Filter with Predicate
        List<DesktopIncident> highSev = store.filter(i -> "HIGH".equalsIgnoreCase(i.getSeverity()) || "CRITICAL".equalsIgnoreCase(i.getSeverity()));
        assertEquals(2, highSev.size(), "Filtered list should have 2 high/critical incidents");

        // Test TreeSet Sorted Extraction
        TreeSet<String> sortedIncidentNumbers = store.extractSortedSet(DesktopIncident::getIncidentNumber);
        assertEquals(3, sortedIncidentNumbers.size());
        assertEquals("INC-2026-0001", sortedIncidentNumbers.first());
        assertEquals("INC-2026-0003", sortedIncidentNumbers.last());

        // Test Iterator-based Removal
        int removed = store.removeMatching(i -> "LOW".equalsIgnoreCase(i.getSeverity()));
        assertEquals(1, removed, "Iterator should remove 1 LOW severity item");
        assertEquals(2, store.size(), "Store size should decrease to 2");
        assertFalse(store.containsKey("INC-2026-0003"), "Removed key should not exist in hash index");
    }

    // =========================================================================
    // TEST 2: Multithreading — EventBatchProcessor
    // =========================================================================
    @Test
    @DisplayName("Multithreading: EventBatchProcessor Producer-Consumer queue & worker thread lifecycle")
    void testMultithreadedBatchProcessing() throws InterruptedException {
        EventBatchProcessor processor = new EventBatchProcessor();
        processor.start();

        List<DesktopActivityLog> testBatch = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            DesktopActivityLog log = new DesktopActivityLog();
            log.setId((long) i);
            log.setActorUsername("test_worker_" + (i % 3));
            log.setAction(i % 4 == 0 ? "ATTENDANCE_RECORD_UNAUTHORIZED" : "MARKS_RECORD");
            log.setOutcome(i % 4 == 0 ? "DENIED" : "ALLOWED");
            testBatch.add(log);
        }

        CountDownLatch enqueueLatch = new CountDownLatch(1);
        processor.enqueueBatch(testBatch, enqueueLatch::countDown);

        // Wait for producer enqueue
        boolean enqueued = enqueueLatch.await(3, TimeUnit.SECONDS);
        assertTrue(enqueued, "Producer thread should successfully enqueue telemetry batch");

        // Allow consumer thread to process
        Thread.sleep(600);

        assertTrue(processor.getTotalProcessed() > 0, "Consumer thread should have processed events");
        processor.shutdown();
    }

    // =========================================================================
    // TEST 3: Syllabus Demo — Legacy Collections (Vector & Hashtable)
    // =========================================================================
    @Test
    @DisplayName("Syllabus Demo: Legacy Vector, Hashtable, and Enumeration traversal")
    void testLegacyCollections() {
        List<String> codes = List.of("RULE-ATT-001", "RULE-MOD-002", "RULE-PRV-003", "RULE-REC-004", "RULE-TIM-005");
        Vector<String> vector = LegacyCollectionsDemo.demonstrateVector(codes);

        assertEquals(5, vector.size());
        assertTrue(vector.capacity() >= 5);
        assertEquals("RULE-ATT-001", vector.firstElement());

        Map<String, String> statusMap = Map.of(
                "CoreEngine", "ONLINE",
                "DatabaseWAL", "ACTIVE",
                "ThreatIndex", "22"
        );
        Hashtable<String, String> table = LegacyCollectionsDemo.demonstrateHashtable(statusMap);

        assertEquals(3, table.size());
        assertEquals("ONLINE", table.get("CoreEngine"));
    }

    // =========================================================================
    // TEST 4: Syllabus Demo — AWT Event Adapter Classes
    // =========================================================================
    @Test
    @DisplayName("Syllabus Demo: WindowAdapter, KeyAdapter, and MouseAdapter")
    void testAwtAdapters() {
        AtomicBoolean windowClosed = new AtomicBoolean(false);
        var winAdapter = AwtAdapterDemo.createWindowCloseAdapter(() -> windowClosed.set(true));
        winAdapter.windowClosing(new WindowEvent(new java.awt.Frame(), WindowEvent.WINDOW_CLOSING));
        assertTrue(windowClosed.get(), "WindowAdapter should trigger windowClosing callback");

        AtomicBoolean keyMatched = new AtomicBoolean(false);
        var keyAdapter = AwtAdapterDemo.createShortcutKeyAdapter(KeyEvent.VK_ENTER, () -> keyMatched.set(true));
        keyAdapter.keyPressed(new KeyEvent(new java.awt.Button(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n'));
        assertTrue(keyMatched.get(), "KeyAdapter should detect target key match");

        AtomicBoolean mouseClicked = new AtomicBoolean(false);
        var mouseAdapter = AwtAdapterDemo.createInspectMouseAdapter(() -> mouseClicked.set(true));
        mouseAdapter.mouseClicked(new MouseEvent(new java.awt.Button(), MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 100, 150, 1, false));
        assertTrue(mouseClicked.get(), "MouseAdapter should detect click event");
    }
}

