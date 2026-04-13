package com.gamilha.services;

import com.gamilha.entity.User;

/**
 * Contexte de session — stocke l'utilisateur connecté.
 * Equivalent du app.user de Symfony / Twig.
 */
public class SessionContext {

    private static User currentUser;

    private SessionContext() {}

    public static void setCurrentUser(User user) { currentUser = user; }

    public static User getCurrentUser() { return currentUser; }

    public static void clear() { currentUser = null; }

    public static boolean isLoggedIn() { return currentUser != null; }
}
