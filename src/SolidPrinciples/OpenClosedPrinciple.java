package SolidPrinciples;

// Software entities(classes, function, modules etc.) should be open for extension but closed
// for modification.
/*
* In Simple Language:
* Open for Extension -> We can add new behaviour or new functionality easily.
* Closed for Modification -> Once the class is written or tested, we should not change its existing code
* when adding new modifications.
* A bad example of this would be if else hell.
* A good example would be added abstraction + polymorphism.
* */

interface PaymentMethod{
    void process(double amount);
}

class CreditCardPayment implements PaymentMethod {
    @Override
    public void process(double amount){
        System.out.println("Processing Credit Card Payment: $"+ amount);
    }
}

class UPIPayment implements PaymentMethod{
    @Override
    public void process(double amount) {
        System.out.println("Processing UPI Payment: $"+ amount);
    }
}

class PaymentProcessor{
    public void processPayment(PaymentMethod paymentMethod, double amount){
        paymentMethod.process(amount);
    }
}

// Now adding new payment Method (Apple Pay)
class ApplePayPayment implements PaymentMethod{
    @Override
    public void process(double amount) {
        System.out.println("Processing Apple Pay payment: $"+ amount);
    }
}

public class OpenClosedPrinciple {
    public static void main(String[] args){
        PaymentProcessor pp = new PaymentProcessor();

        pp.processPayment(new CreditCardPayment(), 100);
        pp.processPayment(new UPIPayment(), 50);

        // Added new type of payment without touching existing code.
        pp.processPayment(new ApplePayPayment(), 200);
    }
}
