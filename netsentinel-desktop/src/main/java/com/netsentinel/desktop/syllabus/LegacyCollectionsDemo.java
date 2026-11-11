package com.netsentinel.desktop.syllabus;

import java.util.*;

/**
 * =========================================================================
 * NETSENTINEL SYLLABUS DEMO: LEGACY COLLECTIONS (Vector & Hashtable)
 * =========================================================================
 * 
 * Demonstrates:
 * 1. java.util.Vector (JDK 1.0 synchronized re-sizable array)
 * 2. java.util.Hashtable (JDK 1.0 synchronized key-value dictionary)
 * 3. java.util.Enumeration (predecessor to Iterator)
 * 
 * Academic Syllabus Justification:
 * Standard university Java curricula teach Vector and Hashtable to illustrate
 * the historical progression of Java Collections. In modern enterprise applications,
 * Vector is superseded by ArrayList (or Collections.synchronizedList / CopyOnWriteArrayList)
 * and Hashtable is superseded by HashMap (or ConcurrentHashMap). This class exists
 * specifically to provide concrete runnable code for syllabus mapping and viva assessment.
 */
public class LegacyCollectionsDemo {

    /**
     * Demonstrates Vector operations: element addition, capacity expansion, and Enumeration.
     */
    public static Vector<String> demonstrateVector(List<String> sampleCodes) {
        // Initial capacity 4, capacityIncrement 2
        Vector<String> vector = new Vector<>(4, 2);

        for (String code : sampleCodes) {
            vector.addElement(code); // Legacy method
        }

        System.out.println("[Syllabus Demo] Vector capacity: " + vector.capacity() + ", size: " + vector.size());

        // Demonstrates Enumeration traversal (JDK 1.0)
        Enumeration<String> enumeration = vector.elements();
        while (enumeration.hasMoreElements()) {
            String elem = enumeration.nextElement();
            System.out.println("  Enumerated Vector Item: " + elem);
        }

        return vector;
    }

    /**
     * Demonstrates Hashtable operations: key-value storage, null-rejection, and key Enumeration.
     */
    public static Hashtable<String, String> demonstrateHashtable(Map<String, String> incidentMap) {
        // Hashtable is synchronized on every method call and rejects null keys/values
        Hashtable<String, String> table = new Hashtable<>();

        for (Map.Entry<String, String> entry : incidentMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                table.put(entry.getKey(), entry.getValue());
            }
        }

        // Enumerate keys
        Enumeration<String> keys = table.keys();
        while (keys.hasMoreElements()) {
            String k = keys.nextElement();
            System.out.println("  Hashtable Entry: " + k + " => " + table.get(k));
        }

        return table;
    }
}

