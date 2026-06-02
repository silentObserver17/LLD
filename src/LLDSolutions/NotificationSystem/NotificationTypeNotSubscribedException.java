package LLDSolutions.NotificationSystem;

public class NotificationTypeNotSubscribedException extends RuntimeException {
    public NotificationTypeNotSubscribedException(String message) {
        super(message);
    }
}
