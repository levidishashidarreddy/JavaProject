package com.netsentinel.desktop;

/**
 * Bootstrap Launcher for NetSentinel JavaFX Desktop Security Console.
 * 
 * Demonstrates:
 * - Decoupling the JVM entry point from javafx.application.Application
 * - Enables running shaded executable fat JARs seamlessly via 'java -jar'
 *   without requiring external --module-path or JavaFX SDK installations.
 */
public class DesktopLauncher {

    public static void main(String[] args) {
        NetSentinelDesktopApp.main(args);
    }
}

