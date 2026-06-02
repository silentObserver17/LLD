package LLDSolutions.NotificationSystem;

public class Main {
    public static void main(String[] args) {
        User user = new User(1, "John Doe", "johndoe@email.com", "9876543210", "devicetoken001");
        NotificationChannel channel1 = new EmailNotification();
        NotificationType notiType1 = NotificationType.TRANSACTIONAL;

        Subscription sub1 = new Subscription(user);
        sub1.subscribe(channel1);
        sub1.addType(notiType1);

        NotificationOrchestrator no1 = NotificationOrchestrator.getInstance();
        no1.addUserSubscription(user, sub1);

        Message message = new Message("Email OTP", "Your OTP is 123456");

        no1.send(user, NotificationType.TRANSACTIONAL, message);

        User user2 = new User(2, "Second User", "seconduser@email.com", "1478523690", "deviceToken002");
        Subscription sub2 = new Subscription(user2);
        sub2.addType(NotificationType.PROMOTIONAL);
        sub2.addType(NotificationType.TRANSACTIONAL);

        sub2.subscribe(new SMSNotification());
        sub2.subscribe(new PushNotification());

        no1.addUserSubscription(user2, sub2);

        Message message2 = new Message("Transactional Message", "Transactional Message to the second user.");
        no1.send(user2, NotificationType.TRANSACTIONAL, message2);

        Message message3 = new Message("SMS Message", "SMS Message to the second user.");
        no1.send(user2, NotificationType.TRANSACTIONAL, message3);
    }
}
