package LLDSolutions.NotificationSystem;

public class PushNotification implements NotificationChannel{
    @Override
    public void send(User user, Message message) {
        System.out.println("PUSH Notification sent to User: " + user.getName() + " with message title " + message.getTitle() + " and message detail: " + message.getBody());

    }
}
