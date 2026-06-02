package LLDSolutions.NotificationSystem;

public class ChannelAlreadySubscribedException extends RuntimeException {
    public ChannelAlreadySubscribedException(String message) {
        super(message);
    }
}

