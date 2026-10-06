package com.sims;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point.
 *
 * <p>Responsibilities (Phase 0 stub):
 * <ul>
 *   <li>Apply the OS-native Look-and-Feel so Swing renders consistently
 *       on Windows, macOS, and Linux without custom painting.</li>
 *   <li>Launch the Login window on the Event Dispatch Thread (EDT).</li>
 * </ul>
 *
 * <p><b>Design Pattern – Behavioral (Observer)</b>: This class delegates all
 * UI initialisation to {@code SwingUtilities.invokeLater}, ensuring that the
 * AWT Event Queue acts as the sole publisher of GUI-construction events and
 * that all registered ActionListeners (observers) are wired on the EDT.
 */
public class Main {

    public static void main(String[] args) {

        // ── 1. Apply native system Look-and-Feel ────────────────────────────
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback: Metal L&F already set by default — log and continue.
            System.err.println("[WARN] Could not apply system L&F: " + e.getMessage());
        }

        // ── 2. Bootstrap UI on the Event Dispatch Thread ─────────────────────
        // All Swing component creation / mutation MUST happen on the EDT.
        SwingUtilities.invokeLater(() -> {
            // TODO (Phase 1): replace stub with LoginFrame instantiation.
            System.out.println("SIMS – Student Information Management System");
            System.out.println("Phase 0 scaffold running. Implement LoginFrame in Phase 1.");
        });
    }
}
