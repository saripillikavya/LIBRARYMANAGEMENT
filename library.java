
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class library extends JFrame {

    7
    // --- MySQL Connection ---
    private static final String URL = "jdbc:mysql://localhost:3306/librarydb";
    private static final String USER = "root"; // your MySQL username
    private static final String PASSWORD = "Pravalika@123"; // your MySQL password
    Connection conn;

    // --- GUI Components ---
    CardLayout cardLayout;
    JPanel mainPanel;

    // Login
    JTextField usernameField;
    JPasswordField passwordField;
    JLabel loginMessage;

    // Dashboard
    DefaultTableModel tableModel;
    JTable table;

    public library() {
        setTitle("Library Management System (MySQL)");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Connect to MySQL
        connectDB();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createLoginPanel(), "login");
        mainPanel.add(createDashboardPanel(), "dashboard");

        add(mainPanel);
        cardLayout.show(mainPanel, "login");
    }

    private void connectDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to MySQL database!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Database Connection Failed!\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(230, 240, 255));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel title = new JLabel("Admin Login");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Username:"), gbc);

        usernameField = new JTextField(15);
        gbc.gridx = 1;
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);

        passwordField = new JPasswordField(15);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        JButton loginBtn = new JButton("Login");
        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(loginBtn, gbc);

        loginMessage = new JLabel("");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(loginMessage, gbc);

        loginBtn.addActionListener(e -> checkLogin());

        return panel;
    }

    private void checkLogin() {
        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());

        if (user.equals("admin") && pass.equals("admin123")) {
            cardLayout.show(mainPanel, "dashboard");
            refreshTable();
        } else {
            loginMessage.setText("Invalid login!");
            loginMessage.setForeground(Color.RED);
        }
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Library Dashboard (MySQL)", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 22));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"ID", "Title", "Total Copies", "Borrowed", "Available"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton addBookBtn = new JButton("Add Book");
        JButton borrowBookBtn = new JButton("Borrow Book");
        JButton returnBookBtn = new JButton("Return Book");
        JButton refreshBtn = new JButton("Refresh");

        buttonPanel.add(addBookBtn);
        buttonPanel.add(borrowBookBtn);
        buttonPanel.add(returnBookBtn);
        buttonPanel.add(refreshBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        addBookBtn.addActionListener(e -> addBook());
        borrowBookBtn.addActionListener(e -> borrowBook());
        returnBookBtn.addActionListener(e -> returnBook());
        refreshBtn.addActionListener(e -> refreshTable());

        return panel;
    }

    private void addBook() {
        try {
            String idStr = JOptionPane.showInputDialog("Enter Book ID:");
            String title = JOptionPane.showInputDialog("Enter Book Title:");
            String copiesStr = JOptionPane.showInputDialog("Enter Total Copies:");
            if (idStr == null || title == null || copiesStr == null) {
                return;
            }

            int id = Integer.parseInt(idStr);
            int copies = Integer.parseInt(copiesStr);

            String sql = "INSERT INTO books (id, title, totalCopies, borrowedCopies) VALUES (?, ?, ?, 0)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.setString(2, title);
            ps.setInt(3, copies);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Book added successfully!");
            refreshTable();
        } catch (SQLIntegrityConstraintViolationException ex) {
            JOptionPane.showMessageDialog(this, "⚠ Book ID already exists!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Error adding book: " + ex.getMessage());
        }
    }

    private void borrowBook() {
        try {
            String idStr = JOptionPane.showInputDialog("Enter Book ID to Borrow:");
            if (idStr == null) {
                return;
            }
            int id = Integer.parseInt(idStr);

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM books WHERE id=" + id);
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Book not found!");
                return;
            }

            int total = rs.getInt("totalCopies");
            int borrowed = rs.getInt("borrowedCopies");

            if (borrowed < total) {
                borrowed++;
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE books SET borrowedCopies=? WHERE id=?");
                ps.setInt(1, borrowed);
                ps.setInt(2, id);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "✅ Book borrowed successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "⚠ No copies available!");
            }

            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Error borrowing: " + ex.getMessage());
        }
    }

    private void returnBook() {
        try {
            String idStr = JOptionPane.showInputDialog("Enter Book ID to Return:");
            if (idStr == null) {
                return;
            }
            int id = Integer.parseInt(idStr);

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM books WHERE id=" + id);
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Book not found!");
                return;
            }

            int borrowed = rs.getInt("borrowedCopies");

            if (borrowed > 0) {
                borrowed--;
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE books SET borrowedCopies=? WHERE id=?");
                ps.setInt(1, borrowed);
                ps.setInt(2, id);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "✅ Book returned successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "⚠ No borrowed copies to return!");
            }

            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Error returning: " + ex.getMessage());
        }
    }

    private void refreshTable() {
        try {
            tableModel.setRowCount(0);
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM books");
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                int total = rs.getInt("totalCopies");
                int borrowed = rs.getInt("borrowedCopies");
                int available = total - borrowed;
                tableModel.addRow(new Object[]{id, title, total, borrowed, available});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Error refreshing table: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new library().setVisible(true));
    }
}
