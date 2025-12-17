package DesignPatterns.Structural.Bridge;

/*
* Bridge is a structural design pattern that decouples an abstraction from its implementation so that the two can vary independently.
* In other words: Instead of tying abstraction and implementation together, Bridge uses composition to let them evolve separately.
* Without Bridge → explosion of subclasses:
*
* */

// Perfect Real-World Example: Payment Gateway with Multiple Processors
//Let’s say we support:

// Abstractions: Razorpay, Stripe, PayPal (different gateway APIs)
// Implementors: TestMode (sandbox), LiveMode (real money)
//With Bridge → 3 gateways + 2 modes = 5 classes, and we can mix at runtime!
//Without Bridge → RazorpayTest, RazorpayLive, StripeTest, StripeLive → 6 classes

record PaymentResult(boolean success, String transactionId, String message) {}
interface PaymentProcessor{
    PaymentResult executePayment(double amount, String currency);
}

// 2. Concrete Implementors
class TestProcessor implements PaymentProcessor {
    @Override
    public PaymentResult executePayment(double amount, String currency) {
        System.out.println("   [TestMode] Simulated payment of " + amount + " " + currency);
        return new PaymentResult(true, "test_txn_" + System.nanoTime(), "Test success");
    }
}

class LiveProcessor implements PaymentProcessor {
    public LiveProcessor() {
        System.out.println("   [LiveMode] Connecting to bank gateway... (secure init)");
    }

    @Override
    public PaymentResult executePayment(double amount, String currency) {
        System.out.println("   [LiveMode] Real payment of " + amount + " " + currency);
        return new PaymentResult(true, "live_txn_" + System.nanoTime(), "Live success");
    }
}

// 3. Abstraction – the "what" (gateway brand)
abstract class PaymentGateway{
    protected final PaymentProcessor processor;

    public PaymentGateway(PaymentProcessor processor) {
        this.processor = processor;
    }

    public abstract PaymentResult process(double amount, String currency, String orderId);
}

class RazorPayGateway extends PaymentGateway{
    public RazorPayGateway(PaymentProcessor processor) {
        super(processor);
    }

    @Override
    public PaymentResult process(double amount, String currency, String orderId) {
        System.out.println("RazorpayGateway: Preparing order " + orderId);
        // Razorpay-specific logic (signature, webhook setup, etc.)
        return processor.executePayment(amount, currency);
    }
}

class StripeGateway extends PaymentGateway{
    public StripeGateway(PaymentProcessor processor) {
        super(processor);
    }

    @Override
    public PaymentResult process(double amount, String currency, String orderId) {
        System.out.println("StripeGateway: Creating intent for " + orderId);
        // Stripe-specific logic (intent, 3DS, etc.)
        return processor.executePayment(amount, currency);
    }
}

class PayPalGateway extends PaymentGateway {
    public PayPalGateway(PaymentProcessor processor) {
        super(processor);
    }

    @Override
    public PaymentResult process(double amount, String currency, String orderId) {
        System.out.println("PayPalGateway: Redirecting user for " + orderId);
        return processor.executePayment(amount, currency);
    }
}

public class Bridge {
    public static void main(String[] args) {
        // Test environment
        PaymentProcessor test = new TestProcessor();

        PaymentGateway razorpayTest = new RazorPayGateway(test);
        PaymentGateway stripeTest = new StripeGateway(test);

        System.out.println("=== TESTING PHASE ===");
        razorpayTest.process(1999.00, "INR", "ORD001");
        stripeTest.process(49.99, "USD", "ORD002");

        // Go live!
        PaymentProcessor live = new LiveProcessor();

        System.out.println("\n=== PRODUCTION PHASE ===");
        PaymentGateway razorpayLive = new RazorPayGateway(live);
        PaymentGateway paypalLive = new PayPalGateway(live);

        razorpayLive.process(4999.00, "INR", "ORD100");
        paypalLive.process(99.99, "USD", "ORD101");

        // We can even switch mode at runtime!
        PaymentGateway dynamic = new StripeGateway(test);
        dynamic.process(10.00, "USD", "ORD999");  // test
        // Later: ((StripeGateway) dynamic).changeProcessor(live); if designed
    }
}

