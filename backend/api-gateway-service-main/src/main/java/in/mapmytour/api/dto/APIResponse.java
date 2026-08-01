package in.mapmytour.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class APIResponse<T> {
    private boolean success;
    private int statusCode;
    private String message;
    private T data;
    private List<String> errors;

    public APIResponse() {
    }

    public APIResponse(boolean success, int statusCode, String message, T data, List<String> errors) {
        this.success = success;
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
        this.errors = errors;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public static <T> APIResponseBuilder<T> builder() {
        return new APIResponseBuilder<>();
    }

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
            return new APIResponse<>(success, statusCode, message, data, errors);
        }
    }
}
