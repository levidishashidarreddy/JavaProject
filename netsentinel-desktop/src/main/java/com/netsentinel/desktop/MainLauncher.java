package com.netsentinel.desktop;

/**
 * Standard JVM entry-point for fat JAR execution and CLI launching
 * to bypass JavaFX runtime component checks.
 */
public class MainLauncher {
    public static void main(String[] args) {
        NetSentinelDesktopApp.main(args);
    }
}

