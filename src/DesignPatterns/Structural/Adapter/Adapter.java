package DesignPatterns.Structural.Adapter;

/*
* Adapter Design Pattern is a structural design pattern that allows objects with incompatible interfaces to collaborate.
* It converts interface of class into another interface that client expects. It acts as a bridge between the Target interface (expected by the client) and the Adaptee (an existing class with a different interface). This structural wrapping enables integration and compatibility across diverse systems.
 * Example: USB-C → HDMI adapter → MacBook connects to a projector
* */
record PaymentResult(String transactionId, String status, String message){}

interface PaymentGateway{
    PaymentResult processPayment(double amount, String currency, String orderId);
}

class RazorPay implements PaymentGateway{
    @Override
    public PaymentResult processPayment(double amount, String currency, String orderId) {
        System.out.println("Razorpay: Charging " + amount + " " + currency);
        return new PaymentResult("razp_123", "SUCCESS", "Paid with Razorpay");
    }
}

// The New Incompatible Library: Stripe
class StripeClient{
    public static class Charge { public String id; public String status; public long amount; }
    public static class Error { public String message; }
    public static class Response { public Charge charge; public Error error; }

    // Completely different signature!
    public Response charge(long amountInCents, String currency, String source, String description) {
        System.out.println("Calling actual Stripe API...");
        var resp = new Response();
        resp.charge = new Charge();
        resp.charge.id = "ch_stripe_" + System.nanoTime();
        resp.charge.status = "succeeded";
        resp.charge.amount = amountInCents;
        return resp;
    }
}

// The Adapter, The Magic
class StripeAdapter implements PaymentGateway{
    private final StripeClient stripeClient;

    public StripeAdapter(StripeClient stripeClient){
        this.stripeClient = stripeClient;
    }

    @Override
    public PaymentResult processPayment(double amount, String currency, String orderId) {
        long amountInCents = Math.round(amount * 100);

        StripeClient.Response resp = stripeClient.charge(
                amountInCents,
                currency,
                "tok_visa",           // in real app: from frontend
                "Order: " + orderId
        );

        if (resp.charge != null && "succeeded".equals(resp.charge.status)) {
            return new PaymentResult(resp.charge.id, "SUCCESS", "Paid via Stripe");
        } else {
            String msg = resp.error != null ? resp.error.message : "Unknown error";
            return new PaymentResult(null, "FAILED", msg);
        }
    }
}

class CheckoutService{
    private final PaymentGateway gateway;

    public CheckoutService(PaymentGateway gateway){
        this.gateway = gateway;
    }

    public void checkout(double amount){
        PaymentResult result = gateway.processPayment(amount, "INR", "ORD001");
        System.out.println("Checkout Result: "+ result);
    }
}

public class Adapter {
    public static void main(String[] args){
        CheckoutService razor = new CheckoutService(new RazorPay());

        // New Gateway just with adapter.
        CheckoutService stripeCheckout = new CheckoutService(
                new StripeAdapter(new StripeClient()) // only this line is new.
        );

        System.out.println("=== Using Razorpay ===");
        razor.checkout(1999.00);

        System.out.println("\n=== Using Stripe via Adapter ===");
        stripeCheckout.checkout(1999.00);
    }
}
