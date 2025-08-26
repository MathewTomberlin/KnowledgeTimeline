package middleware.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for model information.
 */
public class Model {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("object")
    private String object = "model";
    
    @JsonProperty("created")
    private Long created;
    
    @JsonProperty("owned_by")
    private String ownedBy;
    
    @JsonProperty("permission")
    private List<Object> permission;
    
    @JsonProperty("root")
    private String root;
    
    @JsonProperty("parent")
    private String parent;
    
    @JsonProperty("knowledge_aware")
    private Boolean knowledgeAware = false;
    
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    // Constructors
    public Model() {}
    
    public Model(String id, String ownedBy, Integer maxTokens) {
        this.id = id;
        this.ownedBy = ownedBy;
        this.maxTokens = maxTokens;
        this.created = System.currentTimeMillis() / 1000;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String object = "model";
        private Long created = System.currentTimeMillis() / 1000;
        private String ownedBy;
        private List<Object> permission;
        private String root;
        private String parent;
        private Boolean knowledgeAware = false;
        private Integer maxTokens;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder object(String object) {
            this.object = object;
            return this;
        }
        
        public Builder created(Long created) {
            this.created = created;
            return this;
        }
        
        public Builder ownedBy(String ownedBy) {
            this.ownedBy = ownedBy;
            return this;
        }
        
        public Builder permission(List<Object> permission) {
            this.permission = permission;
            return this;
        }
        
        public Builder root(String root) {
            this.root = root;
            return this;
        }
        
        public Builder parent(String parent) {
            this.parent = parent;
            return this;
        }
        
        public Builder knowledgeAware(Boolean knowledgeAware) {
            this.knowledgeAware = knowledgeAware;
            return this;
        }
        
        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Model build() {
            Model model = new Model();
            model.id = this.id;
            model.object = this.object;
            model.created = this.created;
            model.ownedBy = this.ownedBy;
            model.permission = this.permission;
            model.root = this.root;
            model.parent = this.parent;
            model.knowledgeAware = this.knowledgeAware;
            model.maxTokens = this.maxTokens;
            return model;
        }
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getObject() {
        return object;
    }
    
    public void setObject(String object) {
        this.object = object;
    }
    
    public Long getCreated() {
        return created;
    }
    
    public void setCreated(Long created) {
        this.created = created;
    }
    
    public String getOwnedBy() {
        return ownedBy;
    }
    
    public void setOwnedBy(String ownedBy) {
        this.ownedBy = ownedBy;
    }
    
    public List<Object> getPermission() {
        return permission;
    }
    
    public void setPermission(List<Object> permission) {
        this.permission = permission;
    }
    
    public String getRoot() {
        return root;
    }
    
    public void setRoot(String root) {
        this.root = root;
    }
    
    public String getParent() {
        return parent;
    }
    
    public void setParent(String parent) {
        this.parent = parent;
    }
    
    public Boolean getKnowledgeAware() {
        return knowledgeAware;
    }
    
    public void setKnowledgeAware(Boolean knowledgeAware) {
        this.knowledgeAware = knowledgeAware;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
}
