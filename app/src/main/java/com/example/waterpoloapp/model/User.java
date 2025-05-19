package com.example.waterpoloapp.model;

import com.google.firebase.firestore.DocumentId;

public class User {
    @DocumentId
    private String id;
    private String email;
    private boolean isAdmin;

    // Üres konstruktor Firestore-hoz
    public User() {
    }

    public User(String email, boolean isAdmin) {
        this.email = email;
        this.isAdmin = isAdmin;
    }

    // Getterek és setterek
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}