
package com.example.waterpoloapp.model;
import com.google.firebase.firestore.DocumentId;

public class Team {
    @DocumentId
    private String id;
    private String name;
    private String logoUrl;

    // Üres konstruktor Firestore-hoz
    public Team() {
    }

    public Team(String name, String logoUrl) {
        this.name = name;
        this.logoUrl = logoUrl;
    }

    // Getterek és setterek
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    @Override
    public String toString() {
        return name;
    }
}