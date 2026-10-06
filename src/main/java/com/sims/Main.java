package com.sims;

import com.sims.view.SplashScreen;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point — Student Information Management System (SIMS).
 *
 * <h2>Startup Sequence (Phase 1)</h2>
 * <ol>
 *   <li>Apply OS-native Look-and-Feel (required by AGENTS.md §6).</li>
 *   <li>Show {@link SplashScreen} for ~2 seconds (no title bar, {@link javax.swing.JWindow}).</li>
 *   <li>{@code SplashScreen}'s internal {@link javax.swing.Timer} dismisses itself
 *       and opens {@link com.sims.view.LoginFrame} on the EDT.</li>
 * </ol>
 *
 * <h2>Design Pattern – Behavioral: Observer</h2>
 * <p>All UI construction is wrapped in {@link SwingUtilities#invokeLater},
 * ensuring the AWT Event Queue is the sole publisher of GUI-construction events
 * and that all registered {@link java.awt.event.ActionListener}s (observers)
 * are wired correctly on the EDT.</p>
 */
public class Main {

    public static void main(String[] args) {

        // ── 1. Apply native system Look-and-Feel ────────────────────────────
        // Must be set before any Swing component is created (AGENTS.md §6).
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback: Metal L&F already set by default — log and continue.
            System.err.println("[WARN] Could not apply system L&F: " + e.getMessage());
        }

        // ── 2. Bootstrap UI on the Event Dispatch Thread ─────────────────────
        // Phase 1: show SplashScreen; it internally schedules LoginFrame display.
        SwingUtilities.invokeLater(SplashScreen::new);
    }
}
