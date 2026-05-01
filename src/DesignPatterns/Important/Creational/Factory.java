package DesignPatterns.Important.Creational;

interface Notification{
    void send(String message);
}

class EmailNotification implements Notification{
    @Override
    public void send(String message){
        System.out.println("Email: " + message);
    }
}

class SMSNotification implements Notification{
    @Override
    public void send(String message){
        System.out.println("SMS: " + message);
    }
}

class PushNotification implements Notification{
    @Override
    public void send(String message){
        System.out.println("Push: " + message);
    }
}

// Abstract Class with Factory methods
abstract class NotificationSender{
    protected abstract Notification createNotification();

    public void send(String message){
        Notification notification = createNotification();
        notification.send(message);
    }
}

//concrete notification sender.
class EmailSender extends NotificationSender {
    @Override
    protected Notification createNotification() {
        return new EmailNotification();
    }
}

class SMSSender extends NotificationSender{
    @Override
    protected Notification createNotification() {
        return new SMSNotification();
    }
}

class PushSender extends NotificationSender{
    @Override
    protected Notification createNotification() {
        return new PushNotification();
    }
}

public class Factory {
    public static void main(String[] args){
        NotificationSender sender = new EmailSender();
        sender.send("Your OTP is 1234");
    }
}
