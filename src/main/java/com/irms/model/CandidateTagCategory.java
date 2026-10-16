package com.irms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CandidateTagCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private List<CandidateTag> tags = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<CandidateTag> getTags() { return tags; }
    public void setTags(List<CandidateTag> tags) { this.tags = tags; }
}
