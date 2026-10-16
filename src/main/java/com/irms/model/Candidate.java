package com.irms.model;

import java.sql.Timestamp;

public class Candidate {
    private final String id;
    private final String name;
    private final String email;
    private final String phone;
    private final String position;
    private final String source;
    private final String cvName;
    private final Timestamp createdAt;

    public Candidate(String id, String name, String email, String phone, String position,
                     String source, String cvName, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.position = position;
        this.source = source;
        this.cvName = cvName;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPosition() { return position; }
    public String getSource() { return source; }
    public String getCvName() { return cvName; }
    public Timestamp getCreatedAt() { return createdAt; }
}
