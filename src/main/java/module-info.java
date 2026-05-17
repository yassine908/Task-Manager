module com.example.smarttaskmanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.json;

    opens com.example.smarttaskmanager to javafx.fxml;
    opens com.example.smarttaskmanager.controller to javafx.fxml;
    opens com.example.smarttaskmanager.model to javafx.base;

    exports com.example.smarttaskmanager;
}