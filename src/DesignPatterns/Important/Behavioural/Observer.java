package DesignPatterns.Important.Behavioural;

import java.util.ArrayList;
import java.util.List;

interface OrderObserver {
    void update(Order order, String event);
}

interface OrderSubject {
    void attach(OrderObserver observer);
    void detach(OrderObserver observer);
    void notifyObservers(String event);
}

class Order implements OrderSubject {
    private final int orderId;
    private final int userId;
    private final double amount;
    private String status;

    private final List<OrderObserver> observers = new ArrayList<>();

    public Order(int orderId, int userId, double amount) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = "PENDING";
    }

    @Override
    public void attach(OrderObserver observer) {
        observers.add(observer);
    }

    @Override
    public void detach(OrderObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String event) {
        for(OrderObserver observer : observers) {
            observer.update(this, event);
        }
    }

    // State changes trigger notifications
    public void confirm() {
        this.status = "CONFIRMED";
        notifyObservers("ORDER_CONFIRMED");
    }


    public void shipped() {
        this.status = "SHIPPED";
        notifyObservers("ORDER_SHIPPED");
    }

    public void cancel() {
        this.status = "CANCELLED";
        notifyObservers("ORDER_CANCELLED");
    }

    // Getters
    public int getOrderId()   { return orderId; }
    public int getUserId()    { return userId; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
}

class EmailObserver implements OrderObserver {
    @Override
    public void update(Order order, String event) {
        switch (event) {
            case "ORDER_CONFIRMED" ->
                    System.out.println("Email: Order #" + order.getOrderId()
                            + " confirmed. Amount: ₹" + order.getAmount());
            case "ORDER_SHIPPED" ->
                    System.out.println("Email: Order #" + order.getOrderId()
                            + " has been shipped!");
            case "ORDER_CANCELLED" ->
                    System.out.println("Email: Order #" + order.getOrderId()
                            + " cancelled. Refund initiated.");
        }
    }
}

class SMSObserver implements OrderObserver {
    @Override
    public void update(Order order, String event) {
        if (event.equals("ORDER_CONFIRMED") || event.equals("ORDER_SHIPPED")) {
            System.out.println("SMS to user " + order.getUserId()
                    + ": Your order #" + order.getOrderId() + " is " + order.getStatus());
        }
    }
}

class InventoryObserver implements OrderObserver {
    @Override
    public void update(Order order, String event) {
        switch (event) {
            case "ORDER_CONFIRMED" ->
                    System.out.println("Inventory: Reserving stock for order #"
                            + order.getOrderId());
            case "ORDER_CANCELLED" ->
                    System.out.println("Inventory: Releasing stock for order #"
                            + order.getOrderId());
        }
    }
}

class AnalyticsObserver implements OrderObserver {
    @Override
    public void update(Order order, String event) {
        System.out.println("Analytics: Tracking event [" + event + "] "
                + "for order #" + order.getOrderId()
                + " value ₹" + order.getAmount());
    }
}

public class Observer {
    public static void main(String[] args) {
        Order order = new Order(7823, 101, 1499.0);

        // Register observers — can be done anywhere, anytime
        order.attach(new EmailObserver());
        order.attach(new SMSObserver());
        order.attach(new InventoryObserver());
        order.attach(new AnalyticsObserver());

        // Trigger state changes — observers react automatically
        order.confirm();

        // Output:
        // Email: Order #7823 confirmed. Amount: ₹1499.0
        // SMS to user 101: Your order #7823 is CONFIRMED
        // Inventory: Reserving stock for order #7823
        // Analytics: Tracking event [ORDER_CONFIRMED] for order #7823 value ₹1499.0

        System.out.println("---");
        order.shipped();

        // Output:
        // Email: Order #7823 has been shipped!
        // SMS to user 101: Your order #7823 is SHIPPED
        // Analytics: Tracking event [ORDER_SHIPPED] for order #7823 value ₹1499.0
    }
}
