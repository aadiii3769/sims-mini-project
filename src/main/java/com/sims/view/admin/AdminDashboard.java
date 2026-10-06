package com.sims.view.admin;

import com.sims.model.User;
import com.sims.util.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main dashboard window for Administrator-role users.
 *
 * <h2>Design Pattern – Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: Each tab and its child panels register
 * {@code ActionListener} callbacks (Observer pattern) against buttons,
 * table selections, and search fields. The dashboard itself acts as the
 * container that wires these observers together without containing
 * business logic — that is delegated to the Controller layer.</p>
 *
 * <h2>Design Pattern – Creational: Factory Method (consumer)</h2>
 * <p>This class is instantiated by {@link com.sims.factory.DashboardFactory},
 * which applies the Factory Method pattern to decouple dashboard creation
 * from the login flow.</p>
 */
public class AdminDashboard extends JFrame {

    private final User          loggedInUser;
    private final JTabbedPane   tabbedPane;

    // ── Panel references (additional panels attached in later phases) ─────────
    private final StudentListPanel studentListPanel;

    public AdminDashboard(User user) {
        super("SIMS — Administrator Dashboard");
        this.loggedInUser    = user;
        this.studentListPanel = new StudentListPanel(this);
        this.tabbedPane       = new JTabbedPane();

        initFrame();
        buildUI();
        registerShutdownHook();
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private void initFrame() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);   // handled in shutdown hook
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        // ── Header bar ──────────────────────────────────────────────────────
        JPanel header = buildHeader();

        // ── Tabs ──────────────────────────────────────────────────────────
        tabbedPane.addTab("👤  Student Management", studentListPanel);
        // Phase 3, 4, 5 will addTab() here

        // ── Status bar ──────────────────────────────────────────────────────
        JPanel statusBar = buildStatusBar();

        // ── Root layout ─────────────────────────────────────────────────────
        setLayout(new BorderLayout());
        add(header,    BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 58, 95));
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel titleLabel = new JLabel("Student Information Management System");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        JLabel userLabel = new JLabel("Logged in as: " + loggedInUser.getFullName() +
                                      "  |  Role: ADMIN");
        userLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        userLabel.setForeground(new Color(180, 210, 255));

        header.add(titleLabel, BorderLayout.WEST);
        header.add(userLabel,  BorderLayout.EAST);
        return header;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(new Color(240, 242, 245));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        JLabel status = new JLabel("Ready");
        status.setFont(new Font("SansSerif", Font.PLAIN, 11));
        status.setForeground(Color.DARK_GRAY);
        bar.add(status);
        return bar;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Registers a windowClosing listener that gracefully closes the shared
     * Oracle JDBC connection before the JVM exits.
     */
    private void registerShutdownHook() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choice = JOptionPane.showConfirmDialog(
                    AdminDashboard.this,
                    "Are you sure you want to exit SIMS?",
                    "Confirm Exit",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                if (choice == JOptionPane.YES_OPTION) {
                    DBConnection.getInstance().close();
                    dispose();
                    System.exit(0);
                }
            }
        });
    }

    /**
     * Exposes the tabbed pane so future phases can attach new panels
     * (Attendance, Marks, Fees, Transcripts) from outside this class.
     *
     * @return the central {@link JTabbedPane}
     */
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }
}
