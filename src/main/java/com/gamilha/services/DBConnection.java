package com.gamilha.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;



public class DBConnection {



    private static final String URL =
            "jdbc:mysql://localhost:3306/defaultdb" +
                    "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";


    private static final String USER = "root";


    private static final String PASSWORD = "";



    private static Connection instance;



    private DBConnection(){

    }



    public static Connection getInstance(){

        try{

            if(

                    instance == null

                            || instance.isClosed()

            ){

                Class.forName(

                        "com.mysql.cj.jdbc.Driver"

                );


                instance = DriverManager.getConnection(

                        URL,
                        USER,
                        PASSWORD
                );


                System.out.println(

                        "Connexion MySQL OK"

                );

            }

        }

        catch(ClassNotFoundException e){

            throw new RuntimeException(

                    "Driver MySQL introuvable",
                    e
            );

        }

        catch(SQLException e){

            throw new RuntimeException(

                    "Connexion impossible : "
                            + e.getMessage(),

                    e
            );

        }


        return instance;

    }



    public static void close(){

        try{

            if(

                    instance != null

                            && !instance.isClosed()

            ){

                instance.close();


                System.out.println(

                        "Connexion MySQL fermée"

                );

            }

        }

        catch(SQLException e){

            System.out.println(

                    "Erreur fermeture connexion : "
                            + e.getMessage()

            );

        }

    }

}