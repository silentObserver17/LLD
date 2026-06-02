package LLDSolutions.NotificationSystem;

public class SMSNotification implements NotificationChannel{
    @Override
    public void send(User user, Message message) {
        System.out.println("SMS Notification sent to User: " + user.getName() + " with message title " + message.getTitle() + " and message detail: " + message.getBody());
    }
}
