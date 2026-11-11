package com.netsentinel.desktop.syllabus;

import java.awt.event.*;

/**
 * =========================================================================
 * NETSENTINEL SYLLABUS DEMO: AWT EVENT ADAPTER CLASSES
 * =========================================================================
 * 
 * Demonstrates:
 * 1. java.awt.event.WindowAdapter
 * 2. java.awt.event.KeyAdapter
 * 3. java.awt.event.MouseAdapter
 * 
 * Academic Syllabus Justification:
 * University examination syllabi explicitly test "Event Adapter Classes".
 * An Adapter class provides default empty implementations of multi-method listener
 * interfaces (e.g. WindowListener has 7 methods; WindowAdapter allows implementing
 * only windowClosing).
 * 
 * In this modern JavaFX / Spring Boot architecture:
 * JavaFX uses modern single-method SAM functional interfaces (e.g., EventHandler<T>)
 * and Lambda Expressions. This class provides the academic demonstration required
 * by examiners without polluting the JavaFX UI architecture.
 */
public class AwtAdapterDemo {

    /**
     * Demonstrates WindowAdapter: overrides only windowClosing without implementing all 7 WindowListener methods.
     */
    public static WindowAdapter createWindowCloseAdapter(Runnable onCloseAction) {
        return new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("[Syllabus Demo - WindowAdapter] Intercepted window closing event.");
                if (onCloseAction != null) {
                    onCloseAction.run();
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
                System.out.println("[Syllabus Demo - WindowAdapter] Window opened successfully.");
            }
        };
    }

    /**
     * Demonstrates KeyAdapter: overrides keyPressed without implementing keyTyped or keyReleased.
     */
    public static KeyAdapter createShortcutKeyAdapter(int targetKeyCode, Runnable onMatch) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == targetKeyCode) {
                    System.out.println("[Syllabus Demo - KeyAdapter] Detected target key: " + e.getKeyChar());
                    if (onMatch != null) {
                        onMatch.run();
                    }
                }
            }
        };
    }

    /**
     * Demonstrates MouseAdapter: overrides mouseClicked without implementing mousePressed, mouseReleased, mouseEntered, mouseExited.
     */
    public static MouseAdapter createInspectMouseAdapter(Runnable onClickAction) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("[Syllabus Demo - MouseAdapter] Mouse click at coordinates (" + e.getX() + ", " + e.getY() + ")");
                if (onClickAction != null) {
                    onClickAction.run();
                }
            }
        };
    }
}

