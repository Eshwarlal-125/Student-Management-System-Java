import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class StudentManagementSystem extends JFrame {
    private DefaultTableModel tableModel;
    private JTextField idField, nameField, ageField, gradeField, contactField, classField, sectionField;
    private JTable studentTable;
    private Connection conn;
    private String currentUserRole;
    private String currentUserId;
    private boolean isDarkMode = false;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{10,15}");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private JPanel mainPanel;
    private JTabbedPane tabbedPane;

    public StudentManagementSystem() {
        // Set Nimbus Look and Feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to set Nimbus Look and Feel: " + e.getMessage());
        }

        // Database connection
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/student_management1";
            String user = "root";
            String password = "root";
            conn = DriverManager.getConnection(url, user, password);

            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet databases = metaData.getCatalogs();
            boolean dbExists = false;
            while (databases.next()) {
                if ("student_management1".equalsIgnoreCase(databases.getString(1))) {
                    dbExists = true;
                    break;
                }
            }
            databases.close();
            if (!dbExists) {
                throw new SQLException("Database 'student_management1' does not exist.");
            }

            ResultSet tables = metaData.getTables(null, null, "users", null);
            if (!tables.next()) {
                throw new SQLException("Table 'users' does not exist.");
            }
            tables.close();

        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "MySQL JDBC Driver not found: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Show login dialog
        if (!showLoginDialog()) {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Failed to close database connection: " + e.getMessage());
            }
            System.exit(0);
        }

        // Frame setup
        setTitle("Student Management System");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Main panel
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(240, 240, 245));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header panel
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, new Color(50, 150, 250), 0, getHeight(), new Color(30, 100, 200));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        headerPanel.setPreferredSize(new Dimension(0, 60));
        headerPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel headerLabel = new JLabel("Student Management System");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);

        // Dark mode toggle
        JToggleButton darkModeToggle = new JToggleButton("Dark Mode", false);
        darkModeToggle.setFont(new Font("Arial", Font.PLAIN, 12));
        darkModeToggle.setForeground(Color.WHITE);
        darkModeToggle.setBackground(new Color(50, 150, 250));
        darkModeToggle.setFocusPainted(false);
        darkModeToggle.addActionListener(e -> toggleDarkMode());
        headerPanel.add(darkModeToggle);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 14));
        tabbedPane.setBackground(new Color(255, 255, 255));

        // Student Management Tab
        JPanel studentPanel = createStudentPanel();
        tabbedPane.addTab("Students", studentPanel);

        // Attendance Tab
        if (!currentUserRole.equals("Student")) {
            JPanel attendancePanel = createAttendancePanel();
            tabbedPane.addTab("Attendance", attendancePanel);
        }

        // Fees Tab
        if (currentUserRole.equals("Admin") || currentUserRole.equals("Student")) {
            JPanel feesPanel = createFeesPanel();
            tabbedPane.addTab("Fees", feesPanel);
        }

        // Admission Tab
        if (currentUserRole.equals("Admin")) {
            JPanel admissionPanel = createAdmissionPanel();
            tabbedPane.addTab("Admission", admissionPanel);
        }

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);
        refreshTable();
        setVisible(true);
    }

    private boolean showLoginDialog() {
        JDialog loginDialog = new JDialog(this, "Login", true);
        loginDialog.setSize(350, 250);
        loginDialog.setLocationRelativeTo(this);
        loginDialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JTextField userField = new JTextField(15);
        userField.setFont(new Font("Arial", Font.PLAIN, 14));
        userField.setToolTipText("Enter your username");
        userField.setEditable(true);
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JPasswordField passField = new JPasswordField(15);
        passField.setFont(new Font("Arial", Font.PLAIN, 14));
        passField.setToolTipText("Enter your password");
        passField.setEditable(true);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        RoundedButton loginButton = new RoundedButton("Login");
        RoundedButton cancelButton = new RoundedButton("Cancel");

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        loginDialog.add(userLabel, gbc);
        gbc.gridx = 1;
        loginDialog.add(userField, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        loginDialog.add(passLabel, gbc);
        gbc.gridx = 1;
        loginDialog.add(passField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        loginDialog.add(buttonPanel, gbc);

        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);

        final boolean[] loginSuccessful = {false};

        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                userField.setText("");
                passField.setText("");
                userField.requestFocus();
                return;
            }

            try {
                if (conn == null || conn.isClosed()) {
                    JOptionPane.showMessageDialog(loginDialog, "Database connection lost.", "Error", JOptionPane.ERROR_MESSAGE);
                    loginDialog.dispose();
                    return;
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(loginDialog, "Database connection error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                loginDialog.dispose();
                return;
            }

            try {
                String query = "SELECT role, user_id FROM users WHERE LOWER(username) = LOWER(?) AND LOWER(password) = LOWER(?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    currentUserRole = rs.getString("role");
                    currentUserId = rs.getString("user_id");
                    loginSuccessful[0] = true;
                    loginDialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(loginDialog, "Invalid username or password.", "Error", JOptionPane.ERROR_MESSAGE);
                    userField.setText("");
                    passField.setText("");
                    userField.requestFocus();
                }
                rs.close();
                stmt.close();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(loginDialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                userField.setText("");
                passField.setText("");
                userField.requestFocus();
            }
        });

        cancelButton.addActionListener(e -> loginDialog.dispose());

        loginDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                loginDialog.dispose();
            }
        });

        loginDialog.pack();
        loginDialog.setVisible(true);
        return loginSuccessful[0];
    }

    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        Color bgColor = isDarkMode ? new Color(30, 30, 30) : new Color(240, 240, 245);
        Color panelColor = isDarkMode ? new Color(50, 50, 50) : new Color(255, 255, 255);
        Color textColor = isDarkMode ? Color.WHITE : Color.BLACK;

        // Update main panel and tabbed pane
        mainPanel.setBackground(bgColor);
        tabbedPane.setBackground(bgColor);

        // Recursively update all components
        updateComponentColors(mainPanel, panelColor, textColor);

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void updateComponentColors(Component comp, Color panelColor, Color textColor) {
        if (comp instanceof JPanel) {
            comp.setBackground(panelColor);
            for (Component subComp : ((JPanel) comp).getComponents()) {
                updateComponentColors(subComp, panelColor, textColor);
            }
        } else if (comp instanceof JScrollPane) {
            ((JScrollPane) comp).getViewport().getView().setBackground(panelColor);
            ((JScrollPane) comp).getViewport().getView().setForeground(textColor);
            updateComponentColors(((JScrollPane) comp).getViewport().getView(), panelColor, textColor);
        } else if (comp instanceof JLabel || comp instanceof RoundedButton) {
            comp.setForeground(textColor);
        } else if (comp instanceof JTextField) {
            comp.setForeground(textColor);
            comp.setBackground(isDarkMode ? new Color(70, 70, 70) : Color.WHITE);
            ((JTextField) comp).setCaretColor(textColor);
            ((JTextField) comp).setSelectionColor(isDarkMode ? new Color(100, 100, 255) : new Color(184, 207, 229));
            ((JTextField) comp).setSelectedTextColor(textColor);
        } else if (comp instanceof JComboBox) {
            comp.setForeground(textColor);
            comp.setBackground(isDarkMode ? new Color(70, 70, 70) : Color.WHITE);
            ((JComboBox<?>) comp).getEditor().getEditorComponent().setForeground(textColor);
            ((JComboBox<?>) comp).getEditor().getEditorComponent().setBackground(isDarkMode ? new Color(70, 70, 70) : Color.WHITE);
        } else if (comp instanceof JCheckBox) {
            comp.setForeground(textColor);
            comp.setBackground(panelColor);
        }
    }

    private void handleBackButton() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex >= 0) {
            tabbedPane.remove(selectedIndex);
            if (tabbedPane.getTabCount() == 0) {
                int choice = JOptionPane.showConfirmDialog(this, "No tabs remaining. Log out?", "Log Out", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    try {
                        if (conn != null) conn.close();
                    } catch (SQLException e) {
                        System.err.println("Failed to close database connection: " + e.getMessage());
                    }
                    dispose();
                    new StudentManagementSystem();
                }
            }
        }
    }

    private JPanel createStudentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(255, 255, 255));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(new Color(255, 255, 255));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Student Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String[] labels = {"Student ID:", "Name:", "Age:", "Grade:", "Contact:", "Class:", "Section:"};
        JTextField[] fields = {
                idField = new JTextField(15),
                nameField = new JTextField(15),
                ageField = new JTextField(15),
                gradeField = new JTextField(15),
                contactField = new JTextField(15),
                classField = new JTextField(15),
                sectionField = new JTextField(15)
        };
        String[] tooltips = {
                "Unique student ID",
                "Full name",
                "Age (e.g., 15)",
                "Grade (e.g., 10)",
                "Phone number (10-15 digits)",
                "Class (e.g., 10A)",
                "Section (e.g., A)"
        };

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i;
            JLabel label = new JLabel(labels[i]);
            label.setFont(new Font("Arial", Font.PLAIN, 14));
            inputPanel.add(label, gbc);
            gbc.gridx = 1;
            fields[i].setFont(new Font("Arial", Font.PLAIN, 14));
            fields[i].setToolTipText(tooltips[i]);
            inputPanel.add(fields[i], gbc);
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(255, 255, 255));
        String[] buttonLabels = currentUserRole.equals("Admin") ?
                new String[]{ "Update", "Delete", "Search", "Clear", "Refresh"} :
                new String[]{"Search", "Clear", "Refresh"};
        for (String label : buttonLabels) {
            RoundedButton button = new RoundedButton(label);
            buttonPanel.add(button);
            switch (label) {
                case "Update": button.addActionListener(e -> updateStudent()); break;
                case "Delete": button.addActionListener(e -> deleteStudent()); break;
                case "Search": button.addActionListener(e -> searchStudent()); break;
                case "Clear": button.addActionListener(e -> clearFields()); break;
                case "Refresh": button.addActionListener(e -> refreshTable()); break;
            }
        }
        RoundedButton backButton = new RoundedButton("Back");
        backButton.addActionListener(e -> handleBackButton());
        buttonPanel.add(backButton);

        String[] columns = {"ID", "Name", "Age", "Grade", "Contact", "Class", "Section", "Admission Date"};
        tableModel = new DefaultTableModel(columns, 0);
        studentTable = new JTable(tableModel);
        studentTable.setRowHeight(25);
        studentTable.setFont(new Font("Arial", Font.PLAIN, 12));
        studentTable.setSelectionBackground(new Color(100, 181, 246));
        JScrollPane scrollPane = new JScrollPane(studentTable);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(255, 255, 255));

        // Table for attendance records
        DefaultTableModel attTableModel = new DefaultTableModel(new String[]{"Student ID", "Name", "Date", "Status"}, 0);
        JTable attTable = new JTable(attTableModel);
        attTable.setRowHeight(25);
        JScrollPane tableScrollPane = new JScrollPane(attTable);

        // Carousel panel
        JPanel carouselPanel = new JPanel(new GridBagLayout());
        carouselPanel.setBackground(new Color(255, 255, 255));
        carouselPanel.setBorder(BorderFactory.createTitledBorder("Mark Attendance"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fetch classes
        List<String> classes = new ArrayList<>();
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT DISTINCT class_name FROM students ORDER BY class_name");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                classes.add(rs.getString("class_name"));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load classes: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return panel;
        }

        if (classes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No classes found. Please add students with class names.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return panel;
        }

        // Class selection dropdown
        JLabel classLabel = new JLabel("Class:");
        classLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JComboBox<String> classCombo = new JComboBox<>(classes.toArray(new String[0]));
        classCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        classCombo.setToolTipText("Select a class");

        // Fetch students for the initial class
        List<String[]> students = new ArrayList<>();
        String selectedClass = (String) classCombo.getSelectedItem();
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM students WHERE class_name = ? ORDER BY id");
            stmt.setString(1, selectedClass);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                students.add(new String[]{rs.getString("id"), rs.getString("name")});
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load students: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return panel;
        }

        if (students.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students found for class " + selectedClass + ".", "Info", JOptionPane.INFORMATION_MESSAGE);
            return panel;
        }

        // Student display components
        JLabel studentIdLabel = new JLabel("Student ID: ");
        studentIdLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JLabel studentIdValue = new JLabel(students.get(0)[0]);
        studentIdValue.setFont(new Font("Arial", Font.PLAIN, 14));

        JLabel studentNameLabel = new JLabel("Name: ");
        studentNameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JLabel studentNameValue = new JLabel(students.get(0)[1]);
        studentNameValue.setFont(new Font("Arial", Font.PLAIN, 14));

        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JTextField dateField = new JTextField(LocalDate.now().format(DATE_FORMATTER), 15);
        dateField.setFont(new Font("Arial", Font.PLAIN, 14));
        dateField.setToolTipText("Date (YYYY-MM-DD)");

        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JCheckBox presentCheck = new JCheckBox("Present");
        presentCheck.setFont(new Font("Arial", Font.PLAIN, 14));
        JCheckBox tardyCheck = new JCheckBox("Tardy");
        tardyCheck.setFont(new Font("Arial", Font.PLAIN, 14));
        JCheckBox leftEarlyCheck = new JCheckBox("Left Early");
        leftEarlyCheck.setFont(new Font("Arial", Font.PLAIN, 14));

        // Ensure only one checkbox is selected
        ActionListener checkboxListener = e -> {
            JCheckBox source = (JCheckBox) e.getSource();
            if (source.isSelected()) {
                if (source != presentCheck) presentCheck.setSelected(false);
                if (source != tardyCheck) tardyCheck.setSelected(false);
                if (source != leftEarlyCheck) leftEarlyCheck.setSelected(false);
            }
        };
        presentCheck.addActionListener(checkboxListener);
        tardyCheck.addActionListener(checkboxListener);
        leftEarlyCheck.addActionListener(checkboxListener);

        // Navigation buttons
        RoundedButton prevButton = new RoundedButton("Previous");
        RoundedButton nextButton = new RoundedButton("Next");
        final int[] currentIndex = {0};

        prevButton.addActionListener(e -> {
            if (currentIndex[0] > 0) {
                currentIndex[0]--;
                studentIdValue.setText(students.get(currentIndex[0])[0]);
                studentNameValue.setText(students.get(currentIndex[0])[1]);
                presentCheck.setSelected(false);
                tardyCheck.setSelected(false);
                leftEarlyCheck.setSelected(false);
            }
        });

        nextButton.addActionListener(e -> {
            if (currentIndex[0] < students.size() - 1) {
                currentIndex[0]++;
                studentIdValue.setText(students.get(currentIndex[0])[0]);
                studentNameValue.setText(students.get(currentIndex[0])[1]);
                presentCheck.setSelected(false);
                tardyCheck.setSelected(false);
                leftEarlyCheck.setSelected(false);
            }
        });

        // Class selection listener
        classCombo.addActionListener(e -> {
            String newClass = (String) classCombo.getSelectedItem();
            students.clear();
            try {
                PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM students WHERE class_name = ? ORDER BY id");
                stmt.setString(1, newClass);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    students.add(new String[]{rs.getString("id"), rs.getString("name")});
                }
                rs.close();
                stmt.close();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Failed to load students for class " + newClass + ": " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (students.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No students found for class " + newClass + ".", "Info", JOptionPane.INFORMATION_MESSAGE);
                studentIdValue.setText("");
                studentNameValue.setText("");
                prevButton.setEnabled(false);
                nextButton.setEnabled(false);
                return;
            }

            currentIndex[0] = 0;
            studentIdValue.setText(students.get(0)[0]);
            studentNameValue.setText(students.get(0)[1]);
            presentCheck.setSelected(false);
            tardyCheck.setSelected(false);
            leftEarlyCheck.setSelected(false);
            prevButton.setEnabled(true);
            nextButton.setEnabled(students.size() > 1);
        });

        // Mark attendance button
        RoundedButton markButton = new RoundedButton("Mark Attendance");
        markButton.addActionListener(e -> {
            if (!currentUserRole.equals("Teacher") && !currentUserRole.equals("Admin")) {
                JOptionPane.showMessageDialog(this, "Unauthorized action.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String studentId = studentIdValue.getText();
            String date = dateField.getText().trim();
            String status = presentCheck.isSelected() ? "Present" :
                    tardyCheck.isSelected() ? "Tardy" :
                            leftEarlyCheck.isSelected() ? "Left Early" : null;

            if (studentId.isEmpty() || date.isEmpty() || !DATE_PATTERN.matcher(date).matches()) {
                JOptionPane.showMessageDialog(this, "Invalid date format (YYYY-MM-DD) or no student selected.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (status == null) {
                JOptionPane.showMessageDialog(this, "Please select an attendance status.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO attendance (student_id, date, status) VALUES (?, ?, ?)");
                stmt.setString(1, studentId);
                stmt.setString(2, date);
                stmt.setString(3, status);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Attendance marked for " + studentNameValue.getText(), "Success", JOptionPane.INFORMATION_MESSAGE);
                attTableModel.setRowCount(0);
                refreshAttendanceTable(attTableModel);
                presentCheck.setSelected(false);
                tardyCheck.setSelected(false);
                leftEarlyCheck.setSelected(false);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // View report button
        RoundedButton viewReportButton = new RoundedButton("View Report");
        viewReportButton.addActionListener(e -> {
            String reportType = JOptionPane.showInputDialog(this, "Enter report type (student/date/class):");
            if (reportType == null || reportType.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Report type cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            reportType = reportType.trim().toLowerCase();
            if (!reportType.equals("student") && !reportType.equals("date") && !reportType.equals("class")) {
                JOptionPane.showMessageDialog(this, "Invalid report type. Use 'student', 'date', or 'class'.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String prompt = reportType.equals("student") ? "Enter student ID (e.g., S001):" :
                    reportType.equals("date") ? "Enter date (YYYY-MM-DD):" :
                            "Enter class name (e.g., 10A):";
            String value = JOptionPane.showInputDialog(this, prompt);
            if (value == null || value.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Value cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            value = value.trim();

            // Validate date format for 'date' report type
            if (reportType.equals("date") && !DATE_PATTERN.matcher(value).matches()) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD (e.g., 2025-05-18).", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check database connection
            try {
                if (conn == null || conn.isClosed()) {
                    JOptionPane.showMessageDialog(this, "Database connection lost. Please restart the application.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database connection error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            attTableModel.setRowCount(0);
            try {
                String query = reportType.equals("student") ?
                        "SELECT a.student_id, s.name, a.date, a.status FROM attendance a JOIN students s ON a.student_id = s.id WHERE a.student_id = ?" :
                        reportType.equals("date") ?
                                "SELECT a.student_id, s.name, a.date, a.status FROM attendance a JOIN students s ON a.student_id = s.id WHERE a.date = ?" :
                                "SELECT a.student_id, s.name, a.date, a.status FROM attendance a JOIN students s ON a.student_id = s.id WHERE s.class_name = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, value);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    attTableModel.addRow(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)});
                }
                rs.close();
                stmt.close();
                if (attTableModel.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(this, "No records found for the given " + reportType + ".", "Info", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Back button
        RoundedButton backButton = new RoundedButton("Back");
        backButton.addActionListener(e -> handleBackButton());

        // Layout carousel components
        gbc.gridx = 0; gbc.gridy = 0;
        carouselPanel.add(classLabel, gbc);
        gbc.gridx = 1;
        carouselPanel.add(classCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        carouselPanel.add(studentIdLabel, gbc);
        gbc.gridx = 1;
        carouselPanel.add(studentIdValue, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        carouselPanel.add(studentNameLabel, gbc);
        gbc.gridx = 1;
        carouselPanel.add(studentNameValue, gbc);
        gbc.gridx = 0; gbc.gridy = 3;
        carouselPanel.add(dateLabel, gbc);
        gbc.gridx = 1;
        carouselPanel.add(dateField, gbc);
        gbc.gridx = 0; gbc.gridy = 4;
        carouselPanel.add(statusLabel, gbc);
        gbc.gridx = 1;
        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkPanel.setBackground(new Color(255, 255, 255));
        checkPanel.add(presentCheck);
        checkPanel.add(tardyCheck);
        checkPanel.add(leftEarlyCheck);
        carouselPanel.add(checkPanel, gbc);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel navPanel = new JPanel(new FlowLayout());
        navPanel.setBackground(new Color(255, 255, 255));
        navPanel.add(prevButton);
        navPanel.add(nextButton);
        navPanel.add(markButton);
        carouselPanel.add(navPanel, gbc);

        // Button panel for report and back
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(255, 255, 255));
        buttonPanel.add(viewReportButton);
        buttonPanel.add(backButton);

        panel.add(carouselPanel, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        refreshAttendanceTable(attTableModel);
        return panel;
    }

    private void refreshAttendanceTable(DefaultTableModel model) {
        try {
            model.setRowCount(0);
            PreparedStatement stmt = conn.prepareStatement("SELECT a.student_id, s.name, a.date, a.status FROM attendance a JOIN students s ON a.student_id = s.id");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createFeesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel feesTableModel = new DefaultTableModel(new String[]{"ID", "Student ID", "Class", "Amount", "Status", "Due Date"}, 0);
        JTable feesTable = new JTable(feesTableModel);
        feesTable.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(feesTable);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField feeIdField = new JTextField(15);
        feeIdField.setToolTipText("Fee ID (for payment)");
        JTextField feeStudentIdField = new JTextField(15);
        feeStudentIdField.setToolTipText("Student ID");
        JTextField feeClassField = new JTextField(15);
        feeClassField.setToolTipText("Class (e.g., 10A)");
        JTextField amountField = new JTextField(15);
        amountField.setToolTipText("Amount (e.g., 5000)");
        JTextField dueDateField = new JTextField(15);
        dueDateField.setToolTipText("Due Date (YYYY-MM-DD)");
        dueDateField.setText("YYYY-MM-DD");
        dueDateField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (dueDateField.getText().equals("YYYY-MM-DD")) dueDateField.setText("");
            }
        });

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Fee ID:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(feeIdField, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Student ID:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(feeStudentIdField, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Class:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(feeClassField, gbc);
        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(new JLabel("Amount:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(amountField, gbc);
        gbc.gridx = 0; gbc.gridy = 4;
        inputPanel.add(new JLabel("Due Date:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(dueDateField, gbc);

        JPanel buttonPanel = new JPanel();
        RoundedButton addFeeButton = new RoundedButton("Add Fee");
        RoundedButton payFeeButton = new RoundedButton("Pay Fee");
        RoundedButton receiptButton = new RoundedButton("Generate Receipt");
        RoundedButton backButton = new RoundedButton("Back");
        if (currentUserRole.equals("Admin")) {
            buttonPanel.add(addFeeButton);
            buttonPanel.add(payFeeButton);
            buttonPanel.add(receiptButton);
        } else {
            buttonPanel.add(payFeeButton);
            buttonPanel.add(receiptButton);
        }
        buttonPanel.add(backButton);

        addFeeButton.addActionListener(e -> {
            if (!currentUserRole.equals("Admin")) {
                JOptionPane.showMessageDialog(this, "Unauthorized action.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                String studentId = feeStudentIdField.getText().trim();
                String className = feeClassField.getText().trim();
                String amountStr = amountField.getText().trim();
                String dueDate = dueDateField.getText().trim();
                if (studentId.isEmpty() || className.isEmpty() || amountStr.isEmpty() || dueDate.isEmpty() || !DATE_PATTERN.matcher(dueDate).matches()) {
                    JOptionPane.showMessageDialog(this, "Invalid input or date format (YYYY-MM-DD).", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    JOptionPane.showMessageDialog(this, "Amount must be positive.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO fees (student_id, class_name, amount, status, due_date) VALUES (?, ?, ?, 'Due', ?)");
                stmt.setString(1, studentId);
                stmt.setString(2, className);
                stmt.setDouble(3, amount);
                stmt.setString(4, dueDate);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Fee added.", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearFeeFields(feeIdField, feeStudentIdField, feeClassField, amountField, dueDateField);
                refreshFeesTable(feesTableModel);
            } catch (SQLException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input or database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        payFeeButton.addActionListener(e -> {
            String feeId = feeIdField.getText().trim();
            if (feeId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a fee ID.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                PreparedStatement checkStmt = conn.prepareStatement("SELECT student_id FROM fees WHERE id = ? AND status = 'Due'");
                checkStmt.setString(1, feeId);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "Invalid or paid fee ID.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String studentId = rs.getString("student_id");
                if (currentUserRole.equals("Student") && !studentId.equals(currentUserId)) {
                    JOptionPane.showMessageDialog(this, "Unauthorized action.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                PreparedStatement stmt = conn.prepareStatement("UPDATE fees SET status = 'Paid' WHERE id = ?");
                stmt.setString(1, feeId);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Fee marked as paid.", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearFeeFields(feeIdField, feeStudentIdField, feeClassField, amountField, dueDateField);
                refreshFeesTable(feesTableModel);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        receiptButton.addActionListener(e -> {
            String feeId = feeIdField.getText().trim();
            if (feeId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a fee ID.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            try {
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT f.student_id, s.name, f.class_name, f.amount, f.status, f.due_date FROM fees f JOIN students s ON f.student_id = s.id WHERE f.id = ?");
                stmt.setString(1, feeId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String receipt = "Fee Receipt\n\n" +
                            "Fee ID: " + feeId + "\n" +
                            "Student ID: " + rs.getString("student_id") + "\n" +
                            "Student Name: " + rs.getString("name") + "\n" +
                            "Class: " + rs.getString("class_name") + "\n" +
                            "Amount: " + rs.getDouble("amount") + "\n" +
                            "Status: " + rs.getString("status") + "\n" +
                            "Due Date: " + rs.getString("due_date") + "\n" +
                            "Payment Date: " + (rs.getString("status").equals("Paid") ? LocalDate.now().format(DATE_FORMATTER) : "N/A");
                    JOptionPane.showMessageDialog(this, receipt, "Fee Receipt", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Fee ID not found.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        backButton.addActionListener(e -> handleBackButton());

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        refreshFeesTable(feesTableModel);
        return panel;
    }

    private void clearFeeFields(JTextField... fields) {
        for (JTextField field : fields) {
            field.setText("");
        }
        fields[fields.length - 1].setText("YYYY-MM-DD");
    }

    private void refreshFeesTable(DefaultTableModel model) {
        try {
            model.setRowCount(0);
            String query = currentUserRole.equals("Student") ?
                    "SELECT id, student_id, class_name, amount, status, due_date FROM fees WHERE student_id = ?" :
                    "SELECT id, student_id, class_name, amount, status, due_date FROM fees";
            PreparedStatement stmt = conn.prepareStatement(query);
            if (currentUserRole.equals("Student")) {
                stmt.setString(1, currentUserId);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getDouble(4), rs.getString(5), rs.getString(6)});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createAdmissionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(255, 255, 255));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(new Color(255, 255, 255));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Admission Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String[] labels = {"Student ID:", "Name:", "Age:", "Grade:", "Contact:", "Class:", "Section:", "Admission Date:"};
        JTextField[] fields = {
                new JTextField(15), new JTextField(15), new JTextField(15), new JTextField(15),
                new JTextField(15), new JTextField(15), new JTextField(15), new JTextField(15)
        };
        String[] tooltips = {
                "Unique student ID", "Full name", "Age (e.g., 15)", "Grade (e.g., 10)",
                "Phone number (10-15 digits)", "Class (e.g., 10A)", "Section (e.g., A)", "Date (YYYY-MM-DD)"
        };

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i;
            JLabel label = new JLabel(labels[i]);
            label.setFont(new Font("Arial", Font.PLAIN, 14));
            inputPanel.add(label, gbc);
            gbc.gridx = 1;
            fields[i].setFont(new Font("Arial", Font.PLAIN, 14));
            fields[i].setToolTipText(tooltips[i]);
            if (i == 7) {
                fields[i].setText(LocalDate.now().format(DATE_FORMATTER));
            }
            inputPanel.add(fields[i], gbc);
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(255, 255, 255));
        RoundedButton admitButton = new RoundedButton("Admit Student");
        RoundedButton clearButton = new RoundedButton("Clear");
        RoundedButton backButton = new RoundedButton("Back");
        buttonPanel.add(admitButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(backButton);

        admitButton.addActionListener(e -> {
            try {
                String id = fields[0].getText().trim();
                String name = fields[1].getText().trim();
                String ageText = fields[2].getText().trim();
                String grade = fields[3].getText().trim();
                String contact = fields[4].getText().trim();
                String className = fields[5].getText().trim();
                String section = fields[6].getText().trim();
                String admissionDate = fields[7].getText().trim();

                if (id.isEmpty() || name.isEmpty() || ageText.isEmpty() || grade.isEmpty() || contact.isEmpty() ||
                        className.isEmpty() || section.isEmpty() || admissionDate.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!PHONE_PATTERN.matcher(contact).matches()) {
                    JOptionPane.showMessageDialog(this, "Contact must be a 10-15 digit phone number.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!DATE_PATTERN.matcher(admissionDate).matches()) {
                    JOptionPane.showMessageDialog(this, "Invalid date format (YYYY-MM-DD).", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int age;
                try {
                    age = Integer.parseInt(ageText);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Age must be a number.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (age < 5 || age > 20) {
                    JOptionPane.showMessageDialog(this, "Age must be between 5 and 20.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM students WHERE id = ?");
                checkStmt.setString(1, id);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Student ID already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO students (id, name, age, grade, contact, class_name, section, admission_date, academic_year) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                stmt.setString(1, id);
                stmt.setString(2, name);
                stmt.setInt(3, age);
                stmt.setString(4, grade);
                stmt.setString(5, contact);
                stmt.setString(6, className);
                stmt.setString(7, section);
                stmt.setString(8, admissionDate);
                stmt.setString(9, "2025-2026");
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Student admitted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                for (JTextField field : fields) {
                    field.setText("");
                }
                fields[7].setText(LocalDate.now().format(DATE_FORMATTER));
                refreshTable();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        clearButton.addActionListener(e -> {
            for (JTextField field : fields) {
                field.setText("");
            }
            fields[7].setText(LocalDate.now().format(DATE_FORMATTER));
        });

        backButton.addActionListener(e -> handleBackButton());

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void addStudent() {
        if (!currentUserRole.equals("Admin")) {
            JOptionPane.showMessageDialog(this, "Unauthorized action.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            String ageText = ageField.getText().trim();
            String grade = gradeField.getText().trim();
            String contact = contactField.getText().trim();
            String className = classField.getText().trim();
            String section = sectionField.getText().trim();

            if (id.isEmpty() || name.isEmpty() || ageText.isEmpty() || grade.isEmpty() || contact.isEmpty() || className.isEmpty() || section.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!PHONE_PATTERN.matcher(contact).matches()) {
                JOptionPane.showMessageDialog(this, "Contact must be a 10-15 digit phone number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int age;
            try {
                age = Integer.parseInt(ageText);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Age must be a number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (age < 5 || age > 20) {
                JOptionPane.showMessageDialog(this, "Age must be between 5 and 20.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String admissionDate = LocalDate.now().format(DATE_FORMATTER);

            PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM students WHERE id = ?");
            checkStmt.setString(1, id);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Student ID already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO students (id, name, age, grade, contact, class_name, section, admission_date, academic_year) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setInt(3, age);
            stmt.setString(4, grade);
            stmt.setString(5, contact);
            stmt.setString(6, className);
            stmt.setString(7, section);
            stmt.setString(8, admissionDate);
            stmt.setString(9, "2025-2026");
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Student added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearFields();
            refreshTable();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStudent() {
        if (!currentUserRole.equals("Admin")) {
            JOptionPane.showMessageDialog(this, "Unauthorized action.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            String ageText = ageField.getText().trim();
            String grade = gradeField.getText().trim();
            String contact = contactField.getText().trim();
            String className = classField.getText().trim();
            String section = sectionField.getText().trim();

            if (id.isEmpty() || name.isEmpty() || ageText.isEmpty() || grade.isEmpty() || contact.isEmpty() || className.isEmpty() || section.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!PHONE_PATTERN.matcher(contact).matches()) {
                JOptionPane.showMessageDialog(this, "Contact must be a 10-15 digit phone number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int age;
            try {
                age = Integer.parseInt(ageText);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Age must be a number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (age < 5 || age > 20) {
                JOptionPane.showMessageDialog(this, "Age must be between 5 and 20.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM students WHERE id = ?");
            checkStmt.setString(1, id);
            ResultSet rs = checkStmt.executeQuery();
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Student ID does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE students SET name = ?, age = ?, grade = ?, contact = ?, class_name = ?, section = ? WHERE id = ?");
            stmt.setString(1, name);
            stmt.setInt(2, age);
            stmt.setString(3, grade);
            stmt.setString(4, contact);
            stmt.setString(5, className);
            stmt.setString(6, section);
            stmt.setString(7, id);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Student updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearFields();
            refreshTable();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteStudent() {
        if (!currentUserRole.equals("Admin")) {
            JOptionPane.showMessageDialog(this, "Unauthorized action.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String id = idField.getText().trim();
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a Student ID.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM students WHERE id = ?");
            checkStmt.setString(1, id);
            ResultSet rs = checkStmt.executeQuery();
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Student ID does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PreparedStatement stmt = conn.prepareStatement("DELETE FROM students WHERE id = ?");
            stmt.setString(1, id);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Student deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearFields();
            refreshTable();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchStudent() {
        try {
            String searchTerm = JOptionPane.showInputDialog(this, "Enter Student ID or Name to search:");
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return;
            }

            tableModel.setRowCount(0);
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, name, age, grade, contact, class_name, section, admission_date FROM students WHERE id LIKE ? OR name LIKE ?");
            stmt.setString(1, "%" + searchTerm + "%");
            stmt.setString(2, "%" + searchTerm + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("grade"),
                        rs.getString("contact"),
                        rs.getString("class_name"),
                        rs.getString("section"),
                        rs.getString("admission_date")
                });
            }

            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No students found.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        idField.setText("");
        nameField.setText("");
        ageField.setText("");
        gradeField.setText("");
        contactField.setText("");
        classField.setText("");
        sectionField.setText("");
    }

    private void refreshTable() {
        try {
            tableModel.setRowCount(0);
            String query = currentUserRole.equals("Student") ?
                    "SELECT id, name, age, grade, contact, class_name, section, admission_date FROM students WHERE id = ?" :
                    "SELECT id, name, age, grade, contact, class_name, section, admission_date FROM students";
            PreparedStatement stmt = conn.prepareStatement(query);
            if (currentUserRole.equals("Student")) {
                stmt.setString(1, currentUserId);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("grade"),
                        rs.getString("contact"),
                        rs.getString("class_name"),
                        rs.getString("section"),
                        rs.getString("admission_date")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class RoundedButton extends JButton {
        public RoundedButton(String text) {
            super(text);
            setFont(new Font("Arial", Font.BOLD, 12));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(new Color(70, 170, 255));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(new Color(50, 150, 250));
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            super.paintComponent(g);
            g2.dispose();
        }

        @Override
        public void setBackground(Color bg) {
            super.setBackground(bg == null ? new Color(50, 150, 250) : bg);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentManagementSystem());
    }
}