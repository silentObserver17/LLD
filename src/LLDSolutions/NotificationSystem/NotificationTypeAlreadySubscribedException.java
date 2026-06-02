package LLDSolutions.NotificationSystem;

public class NotificationTypeAlreadySubscribedException extends RuntimeException {
    public NotificationTypeAlreadySubscribedException(String message) {
        super(message);
    }
}
