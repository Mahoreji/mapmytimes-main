// src/main/java/in/mapmytour/customer/exception/ServiceException.java
package in.mapmytour.customer.exception;

public class ServiceException extends RuntimeException {
    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}