package LLDSolutions.NotificationSystem;

import java.util.ArrayList;
import java.util.List;

public class Subscription {
    private User user;
    private List<NotificationChannel> channels;
    private List<NotificationType> type;

    public Subscription(User user) {
        this.user = user;
        this.channels = new ArrayList<NotificationChannel>();
        this.type = new ArrayList<NotificationType>();
    }

    public void subscribe(NotificationChannel channel) {
        for (NotificationChannel ch : this.channels) {
            if(ch.equals(channel)) {
                throw new ChannelAlreadySubscribedException("Channel is already subscribed");
            }
        }

        this.channels.add(channel);
    }

    public void unsubscribe(NotificationChannel channel) {
        for (NotificationChannel ch : this.channels) {
            if(ch.equals(channel)) {
                this.channels.remove(channel);
                return;
            }
        }

        throw new ChannelNotSubscribedException("Channel is not Subscribed");
    }

    public List<NotificationChannel> getChannels() {
        return this.channels;
    }

    public List<NotificationType> getType() {
        return this.type;
    }

    public void addType(NotificationType type) {
        for (NotificationType nt : this.type) {
            if(nt.equals(type)) {
                throw new NotificationTypeAlreadySubscribedException("Notification Type is already subscribed");
            }
        }

        this.type.add(type);
    }

    public void removeType(NotificationType type) {
        for (NotificationType nt : this.type) {
            if(nt.equals(type)) {
                this.type.remove(type);
                return;
            }
        }

        throw new NotificationTypeNotSubscribedException("Notification Type is not Subscribed");

    }
}
