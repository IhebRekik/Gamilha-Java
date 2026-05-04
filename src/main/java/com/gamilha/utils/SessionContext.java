package com.gamilha.utils;

import com.gamilha.entity.User;

/**
 * Gestion de session utilisateur (en mémoire).
 *
 * Utilisation :
 * SessionContext.setCurrentUser(user);
 * SessionContext.getCurrentUser();
 * SessionContext.isLoggedIn();
 * SessionContext.clear(); // logout
 */

public final class SessionContext {

    private static volatile User currentUser;

    private SessionContext(){}


    public static void setCurrentUser(User user){

        currentUser = user;

    }


    public static User getCurrentUser(){

        return currentUser;

    }


    public static boolean isLoggedIn(){

        return currentUser != null;

    }


    public static void clear(){

        currentUser = null;

    }

}