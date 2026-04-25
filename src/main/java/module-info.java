module com.gamilha {

    // ── Java SE ─────────────────────────────────────────
    requires java.desktop;
    requires java.sql;

    // ── JavaFX ─────────────────────────────────────────
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;
    requires javafx.web;

    // ── Bibliothèques tierces ─────────────────────────
    requires jbcrypt;
    requires uk.co.caprica.vlcj;
    requires stripe.java;
    requires mysql.connector.j;
    requires jakarta.mail;
    requires jakarta.activation;

    // autres libs UI
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires ably.java;
    requires java.net.http;
    requires emoji.java;

    // ── Opens (pour FXML) ─────────────────────────
    opens com.gamilha to javafx.fxml;
    opens com.gamilha.controllers to javafx.fxml;
    opens com.gamilha.controllers.Messages to javafx.fxml;
    opens com.gamilha.controllers.abonnement to javafx.fxml;
    opens com.gamilha.controllers.admin to javafx.fxml;
    opens com.gamilha.controllers.coaching to javafx.fxml;
    opens com.gamilha.controllers.inscriptions to javafx.fxml;

    // ── Exports ─────────────────────────────────────────
    exports com.gamilha;
    exports com.gamilha.entity;
    exports com.gamilha.services;
    exports com.gamilha.utils;
    exports com.gamilha.controllers;
    exports com.gamilha.controllers.Messages;
    exports com.gamilha.controllers.abonnement;
    exports com.gamilha.controllers.admin;
    exports com.gamilha.controllers.coaching;
    exports com.gamilha.controllers.inscriptions;
}