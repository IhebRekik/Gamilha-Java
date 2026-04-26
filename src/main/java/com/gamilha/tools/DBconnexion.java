package com.gamilha.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton responsable de la connexion à la base de données MySQL.
 *
 * Utilise le patron Singleton pour garantir qu'une seule connexion
 * est ouverte pendant toute la durée de vie de l'application.
 *
 * Les paramètres de connexion sont lus depuis les variables d'environnement
 * (GEVENT_DB_URL, GEVENT_DB_USER, GEVENT_DB_PASSWORD) avec des valeurs
 * par défaut pointant vers une base locale "gamilha".
 */
public class DBconnexion {

    /** URL JDBC de la base de données (configurable via variable d'environnement). */
    public String url = System.getenv().getOrDefault("GEVENT_DB_URL", "jdbc:mysql://localhost:3306/defaultdb");

    /** Nom d'utilisateur MySQL (défaut : root). */
    public String login = System.getenv().getOrDefault("GEVENT_DB_USER", "root");

    /** Mot de passe MySQL (défaut : vide). */
    public String pwd = System.getenv().getOrDefault("GEVENT_DB_PASSWORD", "");

    /** Objet connexion JDBC partagé dans tout le projet. */
    Connection cnx;

    /** Instance unique du Singleton. */
    public static DBconnexion instance;

    /**
     * Constructeur privé : ouvre la connexion JDBC au démarrage.
     * Appelé une seule fois via {@link #getInstance()}.
     */
    private DBconnexion() {
        try {
            cnx = DriverManager.getConnection(url, login, pwd);
            System.out.println("Connection établie");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Retourne l'objet {@link Connection} JDBC actif.
     * Utilisé par tous les services pour exécuter leurs requêtes SQL.
     *
     * @return la connexion à la base de données
     */
    public Connection getCnx() {
        return cnx;
    }

    /**
     * Point d'accès unique au Singleton.
     * Crée l'instance si elle n'existe pas encore (lazy initialization).
     *
     * @return l'unique instance de DBconnexion
     */
    public static DBconnexion getInstance() {
        if (instance == null) {
            instance = new DBconnexion();
        }
        return instance;
    }
}
