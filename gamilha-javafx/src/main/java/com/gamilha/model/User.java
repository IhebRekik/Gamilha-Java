package com.gamilha.model;

public class User {

    private int     id;
    private String  name;
    private String  email;
    private String  profileImage;
    private String  roles;        // JSON Symfony : ["ROLE_ADMIN"] ou ["ROLE_USER"]
    private boolean isActive;
    private String  banUntil;

    public User() {}

    public User(int id, String name, String email, String profileImage,
                String roles, boolean isActive, String banUntil) {
        this.id           = id;
        this.name         = name;
        this.email        = email;
        this.profileImage = profileImage;
        this.roles        = roles;
        this.isActive     = isActive;
        this.banUntil     = banUntil;
    }

    // ── Méthode métier : détecte ROLE_ADMIN comme Symfony ────────────────
    public boolean isAdmin() {
        return roles != null && roles.contains("ROLE_ADMIN");
    }

    // ── Getters / Setters ─────────────────────────────────────────────────
    public int     getId()                               { return id; }
    public void    setId(int id)                         { this.id = id; }
    public String  getName()                             { return name; }
    public void    setName(String name)                  { this.name = name; }
    public String  getEmail()                            { return email; }
    public void    setEmail(String email)                { this.email = email; }
    public String  getProfileImage()                     { return profileImage; }
    public void    setProfileImage(String profileImage)  { this.profileImage = profileImage; }
    public String  getRoles()                            { return roles; }
    public void    setRoles(String roles)                { this.roles = roles; }
    public boolean isActive()                            { return isActive; }
    public void    setActive(boolean active)             { isActive = active; }
    public String  getBanUntil()                         { return banUntil; }
    public void    setBanUntil(String banUntil)          { this.banUntil = banUntil; }

    @Override
    public String toString() { return name + " <" + email + ">"; }
}
