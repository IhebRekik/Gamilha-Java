package com.gamilha.entity;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class User {

    private int id;
    private String name;
    private String email;
    private String password;
    private String profileImage;
    private String roles = "[\"ROLE_USER\"]";
    private boolean isActive = true;
    private String banUntil;
    private int reports = 0;
    private Timestamp createdAt;

    public User() {
        this.createdAt =
                new Timestamp(System.currentTimeMillis());
    }


    public User(
            int id,
            String name,
            String email,
            String profileImage,
            String roles,
            boolean isActive,
            String banUntil
    ){

        this.id = id;
        this.name = name;
        this.email = email;
        this.profileImage = profileImage;
        this.roles = roles;
        this.isActive = isActive;
        this.banUntil = banUntil;

        this.createdAt =
                new Timestamp(System.currentTimeMillis());

    }



    // ROLE ADMIN
    public boolean isAdmin(){

        return roles != null

                && roles.contains("ROLE_ADMIN");

    }



    // GETTERS / SETTERS

    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id = id;
    }



    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }



    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email = email;
    }



    public String getPassword(){
        return password;
    }

    public void setPassword(String password){
        this.password = password;
    }



    public String getProfileImage(){
        return profileImage;
    }

    public void setProfileImage(String profileImage){
        this.profileImage = profileImage;
    }



    public String getRoles(){
        return roles;
    }

    public void setRoles(String roles){
        this.roles = roles;
    }



    public boolean isActive(){
        return isActive;
    }

    public boolean getActive(){
        return isActive;
    }

    public void setActive(boolean active){
        this.isActive = active;
    }



    public String getBanUntil(){
        return banUntil;
    }

    public void setBanUntil(String banUntil){
        this.banUntil = banUntil;
    }



    public int getReports(){
        return reports;
    }

    public void setReports(int reports){
        this.reports = reports;
    }



    public Timestamp getCreatedAt(){
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt){
        this.createdAt = createdAt;
    }



    @Override
    public String toString(){

        return name + " <" + email + ">";

    }

}