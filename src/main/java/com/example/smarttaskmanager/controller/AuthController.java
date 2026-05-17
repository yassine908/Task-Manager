package com.example.smarttaskmanager.controller;

import com.example.smarttaskmanager.MainApp;
import com.example.smarttaskmanager.database.UserDAO;
import com.example.smarttaskmanager.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AuthController {

    @FXML private TextField loginEmailField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Label loginErrorLabel;

    @FXML private TextField registerFirstNameField;
    @FXML private TextField registerLastNameField;
    @FXML private TextField registerEmailField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label registerErrorLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin() {
        loginErrorLabel.setText("");

        String email = loginEmailField.getText().trim();
        String password = loginPasswordField.getText();

        if (email.isBlank() || password.isBlank()) {
            loginErrorLabel.setText("Adresse mail et mot de passe obligatoires.");
            return;
        }

        try {
            User user = userDAO.login(email, password);
            MainApp.showMainView(user);
        } catch (Exception e) {
            loginErrorLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void handleRegister() {
        registerErrorLabel.setText("");

        String firstName = registerFirstNameField.getText().trim();
        String lastName = registerLastNameField.getText().trim();
        String email = registerEmailField.getText().trim();
        String password = registerPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (firstName.isBlank() || lastName.isBlank() || email.isBlank()
                || password.isBlank() || confirmPassword.isBlank()) {
            registerErrorLabel.setText("Tous les champs sont obligatoires.");
            return;
        }

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            registerErrorLabel.setText("Adresse mail invalide.");
            return;
        }

        if (password.length() < 6) {
            registerErrorLabel.setText("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            registerErrorLabel.setText("Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            User user = userDAO.register(firstName, lastName, email, password);
            MainApp.showMainView(user);
        } catch (Exception e) {
            registerErrorLabel.setText(e.getMessage());
        }
    }
}
