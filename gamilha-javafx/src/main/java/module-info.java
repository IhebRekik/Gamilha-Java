module com.gamilha {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires java.sql;
    requires java.desktop;
    requires mysql.connector.j;

    opens com.gamilha            to javafx.fxml;
    opens com.gamilha.controller to javafx.fxml;
    opens com.gamilha.model      to javafx.fxml;

    exports com.gamilha;
    exports com.gamilha.controller;
    exports com.gamilha.model;
    exports com.gamilha.service;
}
