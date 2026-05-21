package com.example.smarttaskmanager.controller;

import com.example.smarttaskmanager.MainApp;
import com.example.smarttaskmanager.database.TaskDAO;
import com.example.smarttaskmanager.model.Task;
import com.example.smarttaskmanager.model.User;
import com.example.smarttaskmanager.util.GeminiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private BorderPane rootPane;

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
    @FXML private Label currentUserLabel;
    @FXML private PieChart pieChart;
    @FXML private BarChart<String, Number> barChart;

    // ===== CHATBOT =====
    private Stage chatStage;
    private TextArea chatHistory;
    private TextField chatInput;
    private Button chatSendButton;
    private Label chatStatus;

    // ===== DAO =====
    private TaskDAO taskDAO;
    private User currentUser;
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

        taskTable.setItems(taskList);
        totalLabel.setText("0");
        doneLabel.setText("0");
        lateLabel.setText("0");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.taskDAO = new TaskDAO(user.getId());
        currentUserLabel.setText(user.getFullName());
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
        task.setUserId(currentUser.getId());
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
    // MODIFIER UNE TÂCHE
    // ====================================================
    @FXML
    private void handleEditTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Sélectionne une tâche à modifier !");
            return;
        }

        TextField editTitleField = new TextField(safeText(selected.getTitle()));
        editTitleField.getStyleClass().add("input-field");

        TextField editDescField = new TextField(safeText(selected.getDescription()));
        editDescField.getStyleClass().add("input-field");

        ComboBox<String> editCategoryBox = new ComboBox<>(FXCollections.observableArrayList(
                "Travail", "Personnel", "Études", "Autre"
        ));
        editCategoryBox.setValue(selected.getCategory() == null ? "Travail" : selected.getCategory());
        editCategoryBox.getStyleClass().add("input-field");

        ComboBox<String> editPriorityBox = new ComboBox<>(FXCollections.observableArrayList(
                "Haute", "Moyenne", "Basse"
        ));
        editPriorityBox.setValue(selected.getPriority() == null ? "Moyenne" : selected.getPriority());
        editPriorityBox.getStyleClass().add("input-field");

        ComboBox<String> editStatusBox = new ComboBox<>(FXCollections.observableArrayList(
                "À faire", "En cours", "Terminée"
        ));
        editStatusBox.setValue(selected.getStatus() == null ? "À faire" : selected.getStatus());
        editStatusBox.getStyleClass().add("input-field");

        DatePicker editDueDatePicker = new DatePicker(selected.getDueDate());
        editDueDatePicker.getStyleClass().add("input-field");

        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Modifier la tâche");
        dialog.setHeaderText("Modifier : " + safeText(selected.getTitle()));
        if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
            dialog.initOwner(rootPane.getScene().getWindow());
        }

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        VBox content = new VBox(12,
                createFieldGroup("TITRE", editTitleField),
                createFieldGroup("DESCRIPTION", editDescField),
                createFieldGroup("CATÉGORIE", editCategoryBox),
                createFieldGroup("PRIORITÉ", editPriorityBox),
                createFieldGroup("STATUT", editStatusBox),
                createFieldGroup("DATE LIMITE", editDueDatePicker)
        );
        content.setPrefWidth(420);
        dialog.getDialogPane().setContent(content);
        if (rootPane.getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(rootPane.getScene().getStylesheets());
        }

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (editTitleField.getText().trim().isEmpty()) {
                showAlert("⚠️ Erreur", "Le titre est obligatoire !");
                event.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }

            Task updatedTask = new Task();
            updatedTask.setId(selected.getId());
            updatedTask.setUserId(selected.getUserId());
            updatedTask.setTitle(editTitleField.getText().trim());
            updatedTask.setDescription(editDescField.getText());
            updatedTask.setCategory(editCategoryBox.getValue());
            updatedTask.setPriority(editPriorityBox.getValue());
            updatedTask.setStatus(editStatusBox.getValue());
            updatedTask.setDueDate(editDueDatePicker.getValue());
            return updatedTask;
        });

        dialog.showAndWait().ifPresent(updatedTask -> {
            taskDAO.updateTask(updatedTask);
            loadTasks();
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

    @FXML
    private void handleLogout() {
        try {
            if (chatStage != null) {
                chatStage.close();
                chatStage = null;
            }
            MainApp.showAuthView();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de se déconnecter : " + e.getMessage());
        }
    }

    // ====================================================
    // ASSISTANT CHATBOT
    // ====================================================
    @FXML
    private void handleOpenChatBot() {
        if (chatStage == null) {
            createChatBotWindow();
        }

        chatStage.show();
        chatStage.toFront();
        chatInput.requestFocus();
    }

    @FXML
    private void handleSendChat() {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        appendChat("Vous", message);
        chatInput.clear();
        setChatLoading(true);

        javafx.concurrent.Task<String> geminiTask = new javafx.concurrent.Task<>() {
            @Override
            protected String call() {
                return GeminiService.askGemini(buildGeminiPrompt(message));
            }
        };

        geminiTask.setOnSucceeded(event -> {
            appendChat("Gemini", geminiTask.getValue());
            setChatLoading(false);
            chatInput.requestFocus();
        });

        geminiTask.setOnFailed(event -> {
            appendChat("Gemini", "Erreur pendant la réponse IA. Vérifie ta connexion et la clé API.");
            setChatLoading(false);
            chatInput.requestFocus();
        });

        Thread thread = new Thread(geminiTask, "gemini-chat-request");
        thread.setDaemon(true);
        thread.start();
    }

    // ====================================================
    // 📋 CHARGER LES TÂCHES + STATS
    // ====================================================
    private void loadTasks() {
        if (taskDAO == null) {
            return;
        }

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

    private VBox createFieldGroup(String labelText, Control field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("input-label");
        field.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, label, field);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void createChatBotWindow() {
        chatHistory = new TextArea();
        chatHistory.setEditable(false);
        chatHistory.setWrapText(true);
        chatHistory.setPrefHeight(320);
        chatHistory.getStyleClass().add("chat-history");

        chatInput = new TextField();
        chatInput.setPromptText("Ex: Ajouter une tâche finir le rapport demain à 18h");
        chatInput.getStyleClass().add("input-field");
        chatInput.setOnAction(event -> handleSendChat());

        chatSendButton = new Button("Envoyer");
        chatSendButton.getStyleClass().add("btn-primary");
        chatSendButton.setOnAction(event -> handleSendChat());

        HBox inputRow = new HBox(10, chatInput, chatSendButton);
        inputRow.getStyleClass().add("chat-input-row");
        HBox.setHgrow(chatInput, javafx.scene.layout.Priority.ALWAYS);

        Label title = new Label("Gemini Bot");
        title.getStyleClass().add("section-title");

        chatStatus = new Label("Connecté à Gemini");
        chatStatus.getStyleClass().add("chat-status");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox header = new HBox(8, new Label("🤖"), title, spacer, chatStatus);
        header.getStyleClass().add("chat-popup-header");

        VBox content = new VBox(12, header, chatHistory, inputRow);
        content.getStyleClass().add("chat-popup");

        Scene scene = new Scene(content, 520, 430);
        if (rootPane.getScene() != null) {
            scene.getStylesheets().addAll(rootPane.getScene().getStylesheets());
        }

        chatStage = new Stage();
        chatStage.setTitle("Bot - Smart Task Manager");
        chatStage.initModality(Modality.NONE);
        if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
            chatStage.initOwner(rootPane.getScene().getWindow());
        }
        chatStage.setScene(scene);
        chatStage.setMinWidth(420);
        chatStage.setMinHeight(340);
        chatStage.setOnCloseRequest(event -> {
            event.consume();
            chatStage.hide();
        });

        chatHistory.setText("Gemini : Bonjour ! Pose-moi une question sur tes tâches ou ton planning.\n");
    }

    private void appendChat(String sender, String message) {
        chatHistory.appendText("\n" + sender + " : " + message + "\n");
    }

    private void setChatLoading(boolean loading) {
        chatInput.setDisable(loading);
        chatSendButton.setDisable(loading);
        chatStatus.setText(loading ? "Gemini réfléchit..." : "Connecté à Gemini");
    }

    private String buildGeminiPrompt(String message) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                Tu es Gemini, l'assistant IA d'une application JavaFX nommée Smart Task Manager.
                Réponds en français, de façon concise et utile.
                Aide l'utilisateur à organiser, prioriser, comprendre et améliorer ses tâches.
                Si l'utilisateur demande une action qui modifie les données, explique clairement quoi faire dans l'interface.

                Tâches actuelles :
                """);

        List<Task> tasks = taskDAO.getAllTasks();
        if (tasks.isEmpty()) {
            prompt.append("- Aucune tâche enregistrée.\n");
        } else {
            for (Task task : tasks) {
                prompt.append("- ")
                        .append(task.getTitle())
                        .append(" | catégorie: ").append(task.getCategory())
                        .append(" | priorité: ").append(task.getPriority())
                        .append(" | statut: ").append(task.getStatus())
                        .append(" | échéance: ").append(task.getDueDate() == null ? "non définie" : task.getDueDate())
                        .append("\n");
            }
        }

        prompt.append("\nQuestion utilisateur : ").append(message);
        return prompt.toString();
    }
}
