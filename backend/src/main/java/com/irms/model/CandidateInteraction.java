package com.irms.model;

import java.sql.Timestamp;

public class CandidateInteraction {
    private String type;
    private String title;
    private String description;
    private String actorName;
    private Timestamp occurredAt;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public Timestamp getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Timestamp occurredAt) { this.occurredAt = occurredAt; }
}
