package DesignPatterns.Creational.Factory;

// Factory pattern is a creational design pattern that provides an interface for creating objects without specifying their concrete classes, centralizing object creation in a factory method.
/*
*  Rather than calling a constructor directly to create an object, we use a factory method to create that object based on some input or condition.
* We can use the factory pattern when:
* - The code needs to work with multiple types of objects.
* - The decision of which class to instantiate must be made at runtime.
* - The instantiation process must be made at runtime.
 * */

interface PaymentMethod{
    void processPayment(double amount);
}

class CreditCard implements PaymentMethod{
    @Override
    public void processPayment(double amount) {
        System.out.println("Processed $" + amount + " via Credit Card");
    }
}

class UPI implements PaymentMethod {
    public void processPayment(double amount) {
        System.out.println("Processed $" + amount + " via UPI");
    }
}

class CashPayment implements PaymentMethod {
    @Override
    public void processPayment(double amount){
        System.out.println("Received $"+ amount + " in cash");
    }
}

class PaymentFactory {
    public static PaymentMethod getPaymentMethod(String type){
        return switch (type.toLowerCase()){
            case "creditcard", "cc" -> new CreditCard();
            case "upi" -> new UPI();
            case "cash" -> new CashPayment();
            default ->  null;
        };
    }
}

public class Factory {
    public static void main(String[] args){
        PaymentMethod cc = PaymentFactory.getPaymentMethod("cc");
        PaymentMethod cash = PaymentFactory.getPaymentMethod("cash");

        assert cc != null;
        cc.processPayment(100);
        assert cash != null;
        cash.processPayment(105);
    }
}
