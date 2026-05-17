package com.example.smarttaskmanager.database;

import com.example.smarttaskmanager.model.Task;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    private Connection conn = DatabaseConnection.getConnection();

    // Ajouter une tâche
    public void addTask(Task task) {
        String sql = "INSERT INTO tasks (title, description, category, priority, status, due_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getCategory());
            stmt.setString(4, task.getPriority());
            stmt.setString(5, task.getStatus());
            stmt.setDate(6, task.getDueDate() != null ? Date.valueOf(task.getDueDate()) : null);
            stmt.executeUpdate();
            System.out.println("tâche ajoutée !");
        } catch (SQLException e) {
            System.out.println(" Erreur ajout : " + e.getMessage());
        }
    }

    //  Récupérer toutes les tâches
    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY due_date ASC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Task t = new Task();
                t.setId(rs.getInt("id"));
                t.setTitle(rs.getString("title"));
                t.setDescription(rs.getString("description"));
                t.setCategory(rs.getString("category"));
                t.setPriority(rs.getString("priority"));
                t.setStatus(rs.getString("status"));
                Date d = rs.getDate("due_date");
                if (d != null) t.setDueDate(d.toLocalDate());
                tasks.add(t);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lecture : " + e.getMessage());
        }
        return tasks;
    }

    // Supprimer une tâche
    public void deleteTask(int id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            System.out.println("🗑Tâche supprimée !");
        } catch (SQLException e) {
            System.out.println(" Erreur suppression : " + e.getMessage());
        }
    }

    // Mettre à jour le statut
    public void updateStatus(int id, String newStatus) {
        String sql = "UPDATE tasks SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            System.out.println(" Statut mis à jour !");
        } catch (SQLException e) {
            System.out.println(" Erreur mise à jour : " + e.getMessage());
        }
    }
}