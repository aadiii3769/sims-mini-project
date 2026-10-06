package com.sims.view.admin;

import com.sims.controller.StudentController;
import com.sims.model.Student;
import com.sims.model.User;
import com.sims.model.UserRole;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.regex.Pattern;

/**
 * Modal dialog for creating a new student or editing an existing student profile.
 *
 * <h2>Design Pattern – Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: The "Save" and "Cancel" buttons register
 * anonymous {@code ActionListener} lambdas (Observer pattern). The dialog does
 * not know who called it; the caller checks {@link #isSaved()} after
 * {@code setVisible(true)} returns and refreshes its data accordingly.</p>
 *
 * <h2>Modes</h2>
 * <ul>
 *   <li><b>CREATE</b> — {@code existingStudent == null}: shows password field,
 *       calls {@code studentController.registerStudent()}.</li>
 *   <li><b>EDIT</b> — {@code existingStudent != null}: hides password field,
 *       pre-populates all fields, calls {@code studentController.updateStudentProfile()}.</li>
 * </ul>
 */
public class StudentFormDialog extends JDialog {

    // ── Validation regexes ───────────────────────────────────────────────────
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w]{2,})+$");
    private static final Pattern ROLL_PATTERN  =
        Pattern.compile("^[A-Za-z0-9\\-/]{3,20}$");

    // ── Departments list ─────────────────────────────────────────────────────
    private static final String[] DEPARTMENTS = {
        "B.E. Computer Science and Engineering",
        "B.E. Electronics and Communication Engineering",
        "B.E. Electrical and Electronics Engineering",
        "B.E. Mechanical Engineering",
        "B.E. Civil Engineering",
        "B.Tech Information Technology",
        "B.Tech Artificial Intelligence and Data Science",
        "B.Tech Computer Science and Business Systems",
        "M.E. Computer Science and Engineering",
        "MBA", "MCA"
    };

    // ── State ────────────────────────────────────────────────────────────────
    private final StudentController studentController;
    private final Student           existingStudent;   // null in CREATE mode
    private boolean                 saved = false;

    // ── Form fields ──────────────────────────────────────────────────────────
    private JTextField   fullNameField;
    private JTextField   usernameField;
    private JPasswordField passwordField;
    private JTextField   emailField;
    private JTextField   phoneField;
    private JTextField   rollField;
    private JComboBox<String> deptCombo;
    private JSpinner     yearSpinner;
    private JTextField   sectionField;
    private JTextField   addressField;

    // Password row panel — hidden in EDIT mode
    private JPanel       passwordRow;

    public StudentFormDialog(Frame parent, Student existing, StudentController controller) {
        super(parent, existing == null ? "Add New Student" : "Edit Student Profile", true);
        this.existingStudent   = existing;
        this.studentController = controller;

        buildUI();
        if (existing != null) populateFields(existing);

        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    // ── UI Construction ───────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        root.add(buildForm(),    BorderLayout.CENTER);
        root.add(buildButtons(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new TitledBorder(
            existingStudent == null ? "New Student Details" : "Edit Student Details"));

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.EAST;
        lc.insets = new Insets(5, 8, 5, 6);

        GridBagConstraints fc = new GridBagConstraints();
        fc.fill    = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets  = new Insets(5, 0, 5, 8);

        int row = 0;

        // ── User/Auth fields ─────────────────────────────────────────────────
        fullNameField  = new JTextField(22);
        usernameField  = new JTextField(22);
        passwordField  = new JPasswordField(22);
        emailField     = new JTextField(22);
        phoneField     = new JTextField(22);

        addRow(form, lc, fc, row++, "Full Name *",  fullNameField);
        addRow(form, lc, fc, row++, "Username *",   usernameField);

        // Password row — only added to form in CREATE mode to avoid orphan label
        if (existingStudent == null) {
            passwordRow = new JPanel(new BorderLayout());
            passwordRow.add(passwordField, BorderLayout.CENTER);
            addRow(form, lc, fc, row++, "Password *", passwordRow);
        }

        addRow(form, lc, fc, row++, "Email *",      emailField);
        addRow(form, lc, fc, row++, "Phone",         phoneField);

        // ── Academic fields ──────────────────────────────────────────────────
        rollField  = new JTextField(22);
        deptCombo  = new JComboBox<>(DEPARTMENTS);
        yearSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        sectionField  = new JTextField(22);
        addressField  = new JTextField(22);

        // Disable username/roll in EDIT mode (primary identifiers)
        if (existingStudent != null) {
            usernameField.setEditable(false);
            usernameField.setBackground(new Color(240, 240, 240));
            rollField.setEditable(false);
            rollField.setBackground(new Color(240, 240, 240));
        }

        addRow(form, lc, fc, row++, "Roll Number *", rollField);
        addRow(form, lc, fc, row++, "Department *",  deptCombo);
        addRow(form, lc, fc, row++, "Year (1–5) *",  yearSpinner);
        addRow(form, lc, fc, row++, "Section",        sectionField);
        addRow(form, lc, fc, row,   "Address",         addressField);

        return form;
    }

    /** Utility: adds a label + component pair on the given grid row. */
    private void addRow(JPanel p, GridBagConstraints lc, GridBagConstraints fc,
                        int row, String label, JComponent comp) {
        lc.gridx = 0; lc.gridy = row;
        fc.gridx = 1; fc.gridy = row;
        p.add(new JLabel(label), lc);
        p.add(comp,              fc);
    }

    private JPanel buildButtons() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.setPreferredSize(new Dimension(85, 30));

        JButton saveBtn = createStyledButton(
            existingStudent == null ? "Register Student" : "Save Changes",
            new Color(30, 90, 180),
            new Color(45, 115, 215)
        );
        saveBtn.setPreferredSize(new Dimension(135, 30));

        // ── Observer: Save ────────────────────────────────────────────────────
        saveBtn.addActionListener(e -> onSave());

        // ── Observer: Cancel ──────────────────────────────────────────────────
        cancelBtn.addActionListener(e -> dispose());

        bar.add(cancelBtn);
        bar.add(saveBtn);
        return bar;
    }

    /**
     * Creates a custom painted button with anti-aliasing, rounded corners,
     * and rollover/press states that works reliably across all Swing Look and Feels.
     */
    private JButton createStyledButton(String text, Color normalColor, Color hoverColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill;
                if (getModel().isPressed()) {
                    fill = normalColor.darker();
                } else if (getModel().isRollover()) {
                    fill = hoverColor;
                } else {
                    fill = normalColor;
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
        return btn;
    }

    // ── Field Population (EDIT mode) ─────────────────────────────────────────

    private void populateFields(Student s) {
        fullNameField.setText(s.getFullName());
        // Show roll number as the identity reference (not the internal userId)
        usernameField.setText(s.getRollNumber());
        emailField.setText(s.getEmail());
        rollField.setText(s.getRollNumber());

        // Match department combo
        for (int i = 0; i < DEPARTMENTS.length; i++) {
            if (DEPARTMENTS[i].equalsIgnoreCase(s.getDepartment())) {
                deptCombo.setSelectedIndex(i);
                break;
            }
        }

        yearSpinner.setValue(s.getYear());
        sectionField.setText(s.getSection() != null ? s.getSection() : "");
        addressField.setText(s.getAddress() != null ? s.getAddress() : "");
    }

    // ── Save Handler ──────────────────────────────────────────────────────────

    private void onSave() {
        if (!validateFields()) return;

        String fullName   = fullNameField.getText().trim();
        String username   = usernameField.getText().trim();
        String email      = emailField.getText().trim();
        String phone      = phoneField.getText().trim();
        String roll       = rollField.getText().trim();
        String dept       = (String) deptCombo.getSelectedItem();
        int    year       = (Integer) yearSpinner.getValue();
        String section    = sectionField.getText().trim();
        String address    = addressField.getText().trim();

        try {
            if (existingStudent == null) {
                // ── CREATE mode ──────────────────────────────────────────────
                String plainPass = new String(passwordField.getPassword());

                User newUser = new User();
                newUser.setFullName(fullName);
                newUser.setUsername(username);
                newUser.setPasswordHash(plainPass);  // controller will hash this
                newUser.setEmail(email);
                newUser.setPhone(phone.isBlank() ? null : phone);
                newUser.setRole(UserRole.STUDENT);

                Student newStudent = new Student();
                newStudent.setRollNumber(roll);
                newStudent.setDepartment(dept);
                newStudent.setYear(year);
                newStudent.setSection(section.isBlank() ? null : section);
                newStudent.setAddress(address.isBlank() ? null : address);

                studentController.registerStudent(newUser, newStudent);
                JOptionPane.showMessageDialog(this,
                    "Student registered successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            } else {
                // ── EDIT mode ────────────────────────────────────────────────
                existingStudent.setDepartment(dept);
                existingStudent.setYear(year);
                existingStudent.setSection(section.isBlank() ? null : section);
                existingStudent.setAddress(address.isBlank() ? null : address);

                studentController.updateStudentProfile(existingStudent);
                JOptionPane.showMessageDialog(this,
                    "Profile updated successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            }

            saved = true;
            dispose();

        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                "Operation failed:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Validation ───────────────────────────────────────────────────────────

    private boolean validateFields() {
        boolean ok = true;

        ok &= requireNonBlank(fullNameField,  "Full Name");
        ok &= requireNonBlank(emailField,     "Email");
        ok &= requireNonBlank(rollField,      "Roll Number");

        if (existingStudent == null) {
            ok &= requireNonBlank(usernameField, "Username");
            String pass = new String(passwordField.getPassword());
            if (pass.isBlank()) {
                highlight(passwordField, true);
                showError("Password is required.");
                ok = false;
            } else {
                highlight(passwordField, false);
            }
        }

        // Email format
        if (!emailField.getText().trim().isEmpty() &&
            !EMAIL_PATTERN.matcher(emailField.getText().trim()).matches()) {
            highlight(emailField, true);
            showError("Invalid email format (e.g. user@domain.com).");
            ok = false;
        }

        // Roll number format
        if (!rollField.getText().trim().isEmpty() &&
            !ROLL_PATTERN.matcher(rollField.getText().trim()).matches()) {
            highlight(rollField, true);
            showError("Roll number must be 3–20 alphanumeric characters.");
            ok = false;
        }

        return ok;
    }

    private boolean requireNonBlank(JTextField field, String fieldName) {
        if (field.getText().trim().isEmpty()) {
            highlight(field, true);
            showError(fieldName + " is required.");
            return false;
        }
        highlight(field, false);
        return true;
    }

    private void highlight(JComponent comp, boolean error) {
        comp.setBorder(error
            ? BorderFactory.createLineBorder(Color.RED, 2)
            : UIManager.getLookAndFeel().getDefaults()
                       .getBorder("TextField.border"));
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }

    // ── State Query ───────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the user clicked Save and the operation succeeded.
     * The calling panel uses this to decide whether to refresh the student table.
     *
     * @return whether a save operation was completed successfully
     */
    public boolean isSaved() {
        return saved;
    }
}
