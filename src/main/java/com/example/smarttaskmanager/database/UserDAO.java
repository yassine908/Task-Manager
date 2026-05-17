package com.example.smarttaskmanager.database;

import com.example.smarttaskmanager.model.User;
import com.example.smarttaskmanager.util.PasswordUtils;

import java.sql.*;

public class UserDAO {

    private final Connection conn = DatabaseConnection.getConnection();

    public UserDAO() {
        ensureUsersTable();
    }

    public User register(String firstName, String lastName, String email, String password) throws SQLException {
        requireConnection();

        if (emailExists(email)) {
            throw new SQLException("Cette adresse mail est déjà utilisée.");
        }

        String sql = """
                INSERT INTO users (first_name, last_name, email, password_hash)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, normalizeEmail(email));
            stmt.setString(4, PasswordUtils.hashPassword(password));
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return new User(keys.getInt(1), firstName, lastName, normalizeEmail(email));
                }
            }
        }

        throw new SQLException("Compte créé, mais identifiant utilisateur introuvable.");
    }

    public User login(String email, String password) throws SQLException {
        requireConnection();

        String sql = "SELECT id, first_name, last_name, email, password_hash FROM users WHERE email = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizeEmail(email));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && PasswordUtils.verifyPassword(password, rs.getString("password_hash"))) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("email")
                    );
                }
            }
        }

        throw new SQLException("Adresse mail ou mot de passe incorrect.");
    }

    private boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizeEmail(email));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void ensureUsersTable() {
        if (conn == null) {
            return;
        }

        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    first_name VARCHAR(100) NOT NULL,
                    last_name VARCHAR(100) NOT NULL,
                    email VARCHAR(180) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("Erreur création table users : " + e.getMessage());
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private void requireConnection() throws SQLException {
        if (conn == null) {
            throw new SQLException("Connexion à la base de données impossible.");
        }
    }
}
