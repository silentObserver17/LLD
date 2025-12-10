package DesignPatterns.Creational.AbstractFactory;

/*
* The Abstract Design Pattern is a creational design pattern that lets us produce families of objects without specify their concrete class
* Think of it as a "factory of factories".
* When to use it:
* Use it when our system must be independent of how its objects are created and support multiple families of related objects.
* Common use cases:

- UI toolkits (Windows vs macOS look-and-feel)
- Game engines (Fantasy weapons vs Sci-Fi weapons)
- Database drivers (MySQL vs PostgresSQL vs Oracle connections + commands)
- Cross-platform apps (Android vs iOS widgets)
* */

interface Button{
    void paint();
}

interface Checkbox{
    void paint();
}

class WindowButton implements Button{
    @Override
    public void paint() {
        System.out.println("Rendering a button in Windows style.");
    }
}

class MacButton implements Button {
    public void paint() {
        System.out.println("Rendering a button in macOS style");
    }
}

class WindowsCheckbox implements Checkbox {
    public void paint() {
        System.out.println("Rendering a checkbox in Windows style");
    }
}

class MacCheckbox implements Checkbox {
    public void paint() {
        System.out.println("Rendering a checkbox in macOS style");
    }
}

// Abstract Factory
interface GUIFactory{
    Button createButton();
    Checkbox createCheckbox();
}

// Now Concrete classes
class WindowsFactory implements GUIFactory{
    @Override
    public Button createButton(){
        return new WindowButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new WindowsCheckbox();
    }
}

class MacFactory implements GUIFactory{
    @Override
    public Button createButton() {
        return new MacButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new MacCheckbox();
    }
}

// Client code - completely Decoupled.
class Application{
    private Button button;
    private Checkbox checkbox;

    public Application(GUIFactory factory){
        this.button = factory.createButton();
        this.checkbox = factory.createCheckbox();
    }

    public void paint(){
        button.paint();
        checkbox.paint();
    }
}

// Second Example: Payment Services.
interface PaymentGateway{
    void processPayment(double amount);
}

interface Invoice{
    void generateInvoice();
}

// =========== INDIA IMPLEMENTATIONS =================
class RazorPay implements PaymentGateway{
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing INR payment via Razorpay: " + amount);
    }
}

class PayUGateway implements PaymentGateway {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing INR payment via PayU: " + amount);
    }
}

class GSTInvoice implements Invoice{
    @Override
    public void generateInvoice() {
        System.out.println("Generating Invoice as per India norms.");
    }
}

// ============== USA IMPLEMENTATIONS ==============
class PayPalGateway implements PaymentGateway {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing USD payment via PayPal: " + amount);
    }
}

class StripeGateway implements PaymentGateway{
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing USD payment via Stripe: "+ amount);
    }
}

class SalesTaxInvoice implements Invoice {
    @Override
    public void generateInvoice() {
        System.out.println("Generating Invoice as per US norms.");
    }
}

interface RegionFactory{
    PaymentGateway createPaymentGateway(String gatewayType);
    Invoice CreateInvoice();
}

class IndiaFactory implements RegionFactory {
    @Override
    public PaymentGateway createPaymentGateway(String gatewayType) {
        if (gatewayType.equalsIgnoreCase("razorpay")) {
            return new RazorPay();
        } else if (gatewayType.equalsIgnoreCase("payu")) {
            return new PayUGateway();
        }
        throw new IllegalArgumentException("Unsupported Payment Gateway");
    }

    @Override
    public Invoice CreateInvoice() {
        return new GSTInvoice();
    }
}

class USAFactory implements RegionFactory {
    @Override
    public PaymentGateway createPaymentGateway(String gatewayType) {
        if (gatewayType.equalsIgnoreCase("paypal")) {
            return new PayPalGateway();
        } else if (gatewayType.equalsIgnoreCase("stripe")) {
            return new StripeGateway();
        }
        throw new IllegalArgumentException("Unsupported gateway for US: " + gatewayType);
    }

    @Override
    public Invoice CreateInvoice() {
        return new SalesTaxInvoice();
    }
}

class CheckoutService{
    private PaymentGateway paymentGateway;
    private Invoice invoice;
    private String gatewayType;

    public CheckoutService(RegionFactory factory, String gatewayType){
        this.gatewayType = gatewayType;
        this.paymentGateway = factory.createPaymentGateway(this.gatewayType);
        this.invoice = factory.CreateInvoice();
    }

    public void completeOrder(double amount){
        paymentGateway.processPayment(amount);
        invoice.generateInvoice();
    }
}

public class AbstractFactory {
    public static void main(String[] args){
        String os = "Mac";

        GUIFactory factory = os.equals("Mac")? new MacFactory() : new WindowsFactory();

        Application app = new Application(factory);
        app.paint();

//        ============== Payment example initialization  ==================
        CheckoutService indiaCheckout = new CheckoutService(new IndiaFactory(), "RazorPay");
        indiaCheckout.completeOrder(2000);
    }
}
