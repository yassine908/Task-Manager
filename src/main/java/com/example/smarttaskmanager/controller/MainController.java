package com.example.smarttaskmanager.controller;

import com.example.smarttaskmanager.database.TaskDAO;
import com.example.smarttaskmanager.model.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    // ===== FORMULAIRE =====
    @FXML private TextField titleField;
    @FXML private TextField descField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private ComboBox<String> priorityBox;
    @FXML private DatePicker dueDatePicker;

    // ===== TABLE =====
    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, String> colTitle;
    @FXML private TableColumn<Task, String> colCategory;
    @FXML private TableColumn<Task, String> colPriority;
    @FXML private TableColumn<Task, String> colStatus;
    @FXML private TableColumn<Task, LocalDate> colDueDate;

    // ===== STATS =====
    @FXML private Label totalLabel;
    @FXML private Label doneLabel;
    @FXML private Label lateLabel;
    @FXML private Label smartMessage;
    @FXML private PieChart pieChart;
    @FXML private BarChart<String, Number> barChart;

    // ===== DAO =====
    private TaskDAO taskDAO = new TaskDAO();
    private ObservableList<Task> taskList = FXCollections.observableArrayList();

    // ====================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Remplir les ComboBox
        categoryBox.setItems(FXCollections.observableArrayList(
                "Travail", "Personnel", "Études", "Autre"
        ));
        priorityBox.setItems(FXCollections.observableArrayList(
                "Haute", "Moyenne", "Basse"
        ));
        categoryBox.setValue("Travail");
        priorityBox.setValue("Moyenne");

        // Lier les colonnes aux propriétés de Task
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

        // Charger les données
        loadTasks();
    }

    // ====================================================
    // ➕ AJOUTER UNE TÂCHE
    // ====================================================
    @FXML
    private void handleAddTask() {
        String title = titleField.getText().trim();

        if (title.isEmpty()) {
            showAlert("⚠️ Erreur", "Le titre est obligatoire !");
            return;
        }

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(descField.getText());
        task.setCategory(categoryBox.getValue());
        task.setPriority(priorityBox.getValue());
        task.setStatus("À faire");
        task.setDueDate(dueDatePicker.getValue());

        taskDAO.addTask(task);

        // Vider le formulaire
        titleField.clear();
        descField.clear();
        dueDatePicker.setValue(null);

        loadTasks();
    }

    // ====================================================
    // 🗑️ SUPPRIMER UNE TÂCHE
    // ====================================================
    @FXML
    private void handleDelete() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("⚠️ Attention", "Sélectionne une tâche à supprimer !");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Supprimer la tâche : " + selected.getTitle() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                taskDAO.deleteTask(selected.getId());
                loadTasks();
            }
        });
    }

    // ====================================================
    // ✔️ MARQUER COMME TERMINÉE
    // ====================================================
    @FXML
    private void handleMarkDone() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("⚠️ Attention", "Sélectionne une tâche !");
            return;
        }
        taskDAO.updateStatus(selected.getId(), "Terminée");
        loadTasks();
    }

    // ====================================================
    // 🔄 ACTUALISER
    // ====================================================
    @FXML
    private void handleRefresh() {
        loadTasks();
    }

    // ====================================================
    // 📋 CHARGER LES TÂCHES + STATS
    // ====================================================
    private void loadTasks() {
        List<Task> tasks = taskDAO.getAllTasks();
        taskList.setAll(tasks);
        taskTable.setItems(taskList);

        updateStats(tasks);
        updateCharts(tasks);
        updateSmartMessage(tasks);
    }

    // ====================================================
    // 📊 METTRE À JOUR LES COMPTEURS
    // ====================================================
    private void updateStats(List<Task> tasks) {
        long total   = tasks.size();
        long done    = tasks.stream().filter(t -> t.getStatus().equals("Terminée")).count();
        long late    = tasks.stream().filter(t ->
                t.getDueDate() != null &&
                        t.getDueDate().isBefore(LocalDate.now()) &&
                        !t.getStatus().equals("Terminée")
        ).count();

        totalLabel.setText(String.valueOf(total));
        doneLabel.setText(String.valueOf(done));
        lateLabel.setText(String.valueOf(late));
    }

    // ====================================================
    // 🥧 METTRE À JOUR LES GRAPHIQUES
    // ====================================================
    private void updateCharts(List<Task> tasks) {

        // --- PieChart ---
        long done    = tasks.stream().filter(t -> t.getStatus().equals("Terminée")).count();
        long inProg  = tasks.stream().filter(t -> t.getStatus().equals("En cours")).count();
        long todo    = tasks.stream().filter(t -> t.getStatus().equals("À faire")).count();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Terminées ✅", done),
                new PieChart.Data("En cours 🔄", inProg),
                new PieChart.Data("À faire 📌", todo)
        );
        pieChart.setData(pieData);

        // --- BarChart ---
        long travail  = tasks.stream().filter(t -> "Travail".equals(t.getCategory())).count();
        long perso    = tasks.stream().filter(t -> "Personnel".equals(t.getCategory())).count();
        long etudes   = tasks.stream().filter(t -> "Études".equals(t.getCategory())).count();
        long autre    = tasks.stream().filter(t -> "Autre".equals(t.getCategory())).count();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tâches");
        series.getData().add(new XYChart.Data<>("Travail", travail));
        series.getData().add(new XYChart.Data<>("Personnel", perso));
        series.getData().add(new XYChart.Data<>("Études", etudes));
        series.getData().add(new XYChart.Data<>("Autre", autre));

        barChart.getData().clear();
        barChart.getData().add(series);
    }

    // ====================================================
    // 💡 MESSAGE INTELLIGENT
    // ====================================================
    private void updateSmartMessage(List<Task> tasks) {
        if (tasks.isEmpty()) {
            smartMessage.setText("💡 Aucune tâche pour le moment. Commencez par en ajouter une !");
            return;
        }

        long total = tasks.size();
        long done  = tasks.stream().filter(t -> t.getStatus().equals("Terminée")).count();
        long late  = tasks.stream().filter(t ->
                t.getDueDate() != null &&
                        t.getDueDate().isBefore(LocalDate.now()) &&
                        !t.getStatus().equals("Terminée")
        ).count();

        int percent = (int) ((done * 100) / total);

        if (late > 0) {
            smartMessage.setText("⚠️ Attention ! Tu as " + late + " tâche(s) en retard. Traite-les en priorité !");
            smartMessage.setStyle("-fx-text-fill: #f38ba8; -fx-font-size: 13px;" +
                    "-fx-background-color: #313244; -fx-padding: 8; -fx-background-radius: 8;");
        } else if (percent == 100) {
            smartMessage.setText("🎉 Bravo ! Toutes tes tâches sont terminées. Tu es au top !");
            smartMessage.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 13px;" +
                    "-fx-background-color: #313244; -fx-padding: 8; -fx-background-radius: 8;");
        } else if (percent >= 50) {
            smartMessage.setText("💪 Bien joué ! " + percent + "% des tâches terminées. Continue ainsi !");
            smartMessage.setStyle("-fx-text-fill: #89b4fa; -fx-font-size: 13px;" +
                    "-fx-background-color: #313244; -fx-padding: 8; -fx-background-radius: 8;");
        } else {
            smartMessage.setText("📌 Tu as complété " + percent + "% de tes tâches. Allez, on s'y met !");
            smartMessage.setStyle("-fx-text-fill: #fab387; -fx-font-size: 13px;" +
                    "-fx-background-color: #313244; -fx-padding: 8; -fx-background-radius: 8;");
        }
    }

    // ====================================================
    // 🔔 ALERTE
    // ====================================================
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}