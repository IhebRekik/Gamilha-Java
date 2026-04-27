open module Gamilha {
    requires java.desktop;
    requires java.net.http;
    requires java.sql;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;
    requires com.google.gson;
    requires jbcrypt;
    requires uk.co.caprica.vlcj;

    exports com.gamilha;
    exports com.gamilha.controllers.coaching;
    exports com.gamilha.entity;
    exports com.gamilha.services;
    exports com.gamilha.utils;
}
