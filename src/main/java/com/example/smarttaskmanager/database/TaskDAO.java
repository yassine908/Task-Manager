package com.example.smarttaskmanager.database;

import com.example.smarttaskmanager.model.Task;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    private final Connection conn = DatabaseConnection.getConnection();
    private final int userId;

    public TaskDAO(int userId) {
        this.userId = userId;
        if (conn != null) {
            ensureUserColumn();
        }
    }

    // Ajouter une tâche
    public void addTask(Task task) {
        String sql = """
                INSERT INTO tasks (user_id, title, description, category, priority, status, due_date)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        if (conn == null) {
            System.out.println("Connexion base de données indisponible.");
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, task.getTitle());
            stmt.setString(3, task.getDescription());
            stmt.setString(4, task.getCategory());
            stmt.setString(5, task.getPriority());
            stmt.setString(6, task.getStatus());
            stmt.setDate(7, task.getDueDate() != null ? Date.valueOf(task.getDueDate()) : null);
            stmt.executeUpdate();
            System.out.println("tâche ajoutée !");
        } catch (SQLException e) {
            System.out.println(" Erreur ajout : " + e.getMessage());
        }
    }

    //  Récupérer toutes les tâches
    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE user_id = ? ORDER BY due_date ASC";
        if (conn == null) {
            System.out.println("Connexion base de données indisponible.");
            return tasks;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Task t = new Task();
                t.setId(rs.getInt("id"));
                t.setUserId(rs.getInt("user_id"));
                t.setTitle(rs.getString("title"));
                t.setDescription(rs.getString("description"));
                t.setCategory(rs.getString("category"));
                t.setPriority(rs.getString("priority"));
                t.setStatus(rs.getString("status"));
                Date d = rs.getDate("due_date");
                if (d != null) t.setDueDate(d.toLocalDate());
                tasks.add(t);
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Erreur lecture : " + e.getMessage());
        }
        return tasks;
    }

    // Supprimer une tâche
    public void deleteTask(int id) {
        String sql = "DELETE FROM tasks WHERE id = ? AND user_id = ?";
        if (conn == null) {
            System.out.println("Connexion base de données indisponible.");
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            System.out.println("🗑Tâche supprimée !");
        } catch (SQLException e) {
            System.out.println(" Erreur suppression : " + e.getMessage());
        }
    }

    // Mettre à jour le statut
    public void updateStatus(int id, String newStatus) {
        String sql = "UPDATE tasks SET status = ? WHERE id = ? AND user_id = ?";
        if (conn == null) {
            System.out.println("Connexion base de données indisponible.");
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, id);
            stmt.setInt(3, userId);
            stmt.executeUpdate();
            System.out.println(" Statut mis à jour !");
        } catch (SQLException e) {
            System.out.println(" Erreur mise à jour : " + e.getMessage());
        }
    }

    private void ensureUserColumn() {
        if (hasUserIdColumn()) {
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN user_id INT NULL AFTER id");
        } catch (SQLException e) {
            System.out.println("Erreur ajout colonne user_id : " + e.getMessage());
        }
    }

    private boolean hasUserIdColumn() {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'tasks'
                  AND COLUMN_NAME = 'user_id'
                """;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.out.println("Erreur vérification colonne user_id : " + e.getMessage());
            return false;
        }
    }
}
