package com.sims.view.parent;

import com.sims.model.User;
import com.sims.util.DBConnection;
import com.sims.view.LoginFrame;
import com.sims.view.student.AttendanceViewPanel;
import com.sims.view.student.MarksViewPanel;
import com.sims.view.student.PaymentPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main dashboard window for Parent-role users.
 *
 * <h2>Design Pattern - Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: Logout and tab actions are handled by
 * Swing event observers. The attendance child panel resolves the linked
 * student through the controller layer, preserving the required MVC boundary.</p>
 */
public class ParentDashboard extends JFrame {

    private final User loggedInUser;

    public ParentDashboard(User user) {
        super("SIMS - Parent Dashboard");
        this.loggedInUser = user;
        initFrame();
        buildUI();
        registerShutdownHook();
    }

    private void initFrame() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Attendance",      new AttendanceViewPanel(loggedInUser));
        tabs.addTab("Marks",           new MarksViewPanel(loggedInUser));
        tabs.addTab("Fees & Payments", new PaymentPanel(loggedInUser));

        add(buildHeader(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 58, 95));
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel title = new JLabel("Parent Portal");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);
        JLabel userLabel = new JLabel("Logged in as: " + loggedInUser.getFullName() + "  |  Role: PARENT");
        userLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        userLabel.setForeground(new Color(180, 210, 255));
        JButton logoutButton = createLogoutButton();
        rightPanel.add(userLabel);
        rightPanel.add(logoutButton);

        header.add(title, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panel.setBackground(new Color(240, 242, 245));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        JLabel status = new JLabel("Ready");
        status.setFont(new Font("SansSerif", Font.PLAIN, 11));
        panel.add(status);
        return panel;
    }

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to log out?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }

    private JButton createLogoutButton() {
        JButton btn = new JButton("Logout") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill;
                if (getModel().isPressed()) {
                    fill = new Color(160, 35, 35);
                } else if (getModel().isRollover()) {
                    fill = new Color(215, 55, 55);
                } else {
                    fill = new Color(190, 45, 45);
                }
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        btn.addActionListener(e -> handleLogout());
        return btn;
    }

    private void registerShutdownHook() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choice = JOptionPane.showConfirmDialog(
                    ParentDashboard.this,
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
}
