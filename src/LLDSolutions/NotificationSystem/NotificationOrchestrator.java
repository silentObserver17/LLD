package LLDSolutions.NotificationSystem;

import java.util.HashMap;
import java.util.List;

public class NotificationOrchestrator {
    private static volatile NotificationOrchestrator instance;
    private HashMap<Integer, Subscription> subscriptions;

    private NotificationOrchestrator() {
        this.subscriptions = new HashMap<>();
    }

    public static NotificationOrchestrator getInstance() {
        if (instance == null) {
            synchronized (NotificationOrchestrator.class) {
                if (instance == null) {
                    instance = new NotificationOrchestrator();
                }
            }
        }

        return instance;
    }

    public void addUserSubscription(User user, Subscription subscription) {
        this.subscriptions.put(user.getUserid(), subscription);
    }

    public void send(User user, NotificationType type, Message message) {
        if(!this.subscriptions.containsKey(user.getUserid())) {
            throw new IllegalArgumentException("User Subscription does not exist");
        }

        Subscription subs = this.subscriptions.get(user.getUserid());
        List<NotificationChannel> channels = subs.getChannels();
        List<NotificationType> notiType = subs.getType();

        for(NotificationChannel channel : channels) {
            if(notiType.contains(type)) {
                channel.send(user, message);
            }
        }
    }
}
