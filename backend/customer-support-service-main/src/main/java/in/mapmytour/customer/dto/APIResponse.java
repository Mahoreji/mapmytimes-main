package in.mapmytour.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class APIResponse<T> {
    private boolean success;
    private int statusCode;
    private String message;
    private T data;
    private List<String> errors;
    
    // Static factory method for better generic type inference
    public static <T> APIResponseBuilder<T> builder() {
        return new APIResponseBuilder<>();
    }
    
    // Builder class for generic types
    public static class APIResponseBuilder<T> {
        private boolean success;
        private int statusCode;
        private String message;
        private T data;
        private List<String> errors;
        
        public APIResponseBuilder<T> success(boolean success) {
            this.success = success;
            return this;
        }
        
        public APIResponseBuilder<T> statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }
        
        public APIResponseBuilder<T> message(String message) {
            this.message = message;
            return this;
        }
        
        public APIResponseBuilder<T> data(T data) {
            this.data = data;
            return this;
        }
        
        public APIResponseBuilder<T> errors(List<String> errors) {
            this.errors = errors;
            return this;
        }
        
        public APIResponse<T> build() {
            return new APIResponse<T>(success, statusCode, message, data, errors);
        }
    }
}
