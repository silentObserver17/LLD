package DesignPatterns.Creational.Prototype;

/*
* Prototype is a creational design pattern that lets us create new objects by copying on existing object(called the prototype) instead of using new.
* Real life analogy: We have perfectly configured laptop and instead of manually setting up 100 new laptops, we just clone one and make tiny changes.
*
* When to Use Prototype?
 Use it when:

- Creating an object is expensive (lots of database calls, API calls, complex setup)
- Objects are almost identical, only small differences (e.g., different amounts, user IDs)
- We want to hide creation logic from the client
- We have many similar configurations (India Razorpay, India PayU, USA Stripe, USA PayPal)
* */

// Every checkout needs a fresh gateway + invoice → lots of new calls, repeated configuration

import java.util.HashMap;
import java.util.Map;

// 1. Make objects Cloneable
interface PaymentGateway extends Cloneable {
    void ProcessPayment(double amount);
    PaymentGateway clone();
}

interface Invoice extends Cloneable {
    void generateInvoice(double amount);
    Invoice clone();
}

// 2. Concrete Classes.
class RazorPayGateway implements PaymentGateway {
    @Override
    public void ProcessPayment(double amount) {
        System.out.println("Processing ₹" + amount + " via Razorpay (India)");
    }

    @Override
    public PaymentGateway clone() {
        try{
            return (PaymentGateway) super.clone(); // shallow copy.
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

class PayUGateway implements PaymentGateway {
    @Override
    public void ProcessPayment(double amount) {
        System.out.println("Processing ₹" + amount + " via PayU (India)");
    }

    @Override
    public PaymentGateway clone() {
        try{
            return (PaymentGateway) super.clone(); // shallow copy;
        }catch(CloneNotSupportedException e){
            throw new RuntimeException(e);
        }
    }
}

class GSTInvoice implements Invoice {
    private double gstRate = 0.18;

    @Override
    public void generateInvoice(double amount) {
        double tax = amount * gstRate;
        System.out.println("GST Invoice: ₹" + amount + " + ₹" + tax + " tax = ₹" + (amount + tax));
    }

    @Override
    public Invoice clone() {
        try {
            return (Invoice) super.clone();
        }catch (CloneNotSupportedException e){
            throw new RuntimeException(e);
        }
    }
}

// 3. Prototype Registry (the real power!)
class CheckoutPrototypeRegistry{
    private final Map<String, PaymentGateway> gatewayPrototypes = new HashMap<>();
    private final Map<String, Invoice> invoicePrototype = new HashMap<>();

    public CheckoutPrototypeRegistry(){
        gatewayPrototypes.put("razorpay", new RazorPayGateway());
        gatewayPrototypes.put("payu", new PayUGateway());
        invoicePrototype.put("gst", new GSTInvoice());
    }

    public PaymentGateway getGateway(String type){
        return gatewayPrototypes.get(type).clone();
    }

    public Invoice getInvoice(String type){
        return invoicePrototype.get(type).clone();
    }
}

// 4. Clean Checkout
class CheckoutService{
    private final CheckoutPrototypeRegistry registry;

    public CheckoutService(CheckoutPrototypeRegistry registry){
        this.registry = registry;
    }

    public void checkOut(double amount, String gatewayType){
        PaymentGateway gateway = registry.getGateway(gatewayType);
        Invoice invoice = registry.getInvoice("gst");

        gateway.ProcessPayment(amount);
        invoice.generateInvoice(amount);
    }
}

public class Prototype {
    public static void main(String[] args){
        CheckoutPrototypeRegistry registry = new CheckoutPrototypeRegistry();

        CheckoutService service = new CheckoutService(registry);

        service.checkOut(2000, "razorpay");
        service.checkOut(899, "payu");
    }
}
