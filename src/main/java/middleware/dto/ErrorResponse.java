package middleware.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for error responses, following OpenAI's API format.
 */
public class ErrorResponse {
    
    @JsonProperty("error")
    private Error error;
    
    // Constructors
    public ErrorResponse() {}
    
    public ErrorResponse(Error error) {
        this.error = error;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Error error;
        
        public Builder error(Error error) {
            this.error = error;
            return this;
        }
        
        public ErrorResponse build() {
            return new ErrorResponse(error);
        }
    }
    
    // Getters and Setters
    public Error getError() {
        return error;
    }
    
    public void setError(Error error) {
        this.error = error;
    }
    
    /**
     * Inner class for error details.
     */
    public static class Error {
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("param")
        private String param;
        
        @JsonProperty("code")
        private String code;
        
        // Constructors
        public Error() {}
        
        public Error(String type, String message) {
            this.type = type;
            this.message = message;
        }
        
        // Getters and Setters
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getParam() {
            return param;
        }
        
        public void setParam(String param) {
            this.param = param;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
    }
}
