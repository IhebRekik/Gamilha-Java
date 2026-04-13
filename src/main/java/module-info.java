module com.gamilha {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires java.sql;
    requires java.desktop;
    requires mysql.connector.j;

    opens com.gamilha            to javafx.fxml;
    opens com.gamilha.controllers to javafx.fxml;
    opens com.gamilha.entity to javafx.fxml;

    exports com.gamilha;
    exports com.gamilha.controllers;
    exports com.gamilha.entity;
    exports com.gamilha.services;
}
