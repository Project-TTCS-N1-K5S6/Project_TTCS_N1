package com.irms.model;

import java.sql.Timestamp;

/**
 * Model đại diện cho một bản ghi trong bảng email_outbox
 */
public class EmailOutboxItem {
    private String id;
    private String recipient;
    private String subject;
    private String template;
    private String payload;
    private String status;
    private int retryCount;
    private String lastError;
    private Timestamp createdAt;
    private Timestamp sentAt;

    public EmailOutboxItem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getSentAt() { return sentAt; }
    public void setSentAt(Timestamp sentAt) { this.sentAt = sentAt; }
}
