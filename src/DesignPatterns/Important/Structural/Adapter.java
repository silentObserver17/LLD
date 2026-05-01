package DesignPatterns.Important.Structural;

interface PaymentProcessor {
    void processPayment(double amount, String currency);
    void refund(String transactionId);
}

//Step 2 — Existing compatible implementation (no adapter needed):
class RazorpayProcessor implements PaymentProcessor {
    @Override
    public void processPayment(double amount, String currency) {
        System.out.println("Razorpay: Processing " + amount + " " + currency);
    }

    @Override
    public void refund(String transactionId) {
        System.out.println("Razorpay: Refunding txn " + transactionId);
    }
}

//Step 3 — The Adaptee (third-party SDK you can't modify):
// PayPal's SDK — incompatible interface, can't touch this
class PayPalSDK {
    public void makePayment(PayPalRequest request) {
        System.out.println("PayPal SDK: Payment of " + request.getAmount()
                + " " + request.getCurrency());
    }

    public void initiateRefund(PayPalRefundRequest request) {
        System.out.println("PayPal SDK: Refund for order " + request.getOrderId());
    }
}

// PayPal's request objects
class PayPalRequest {
    private double amount;
    private String currency;

    public PayPalRequest(double amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }


    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}

class PayPalRefundRequest {
    private String orderId;

    public PayPalRefundRequest(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}

//Step 4 — The Adapter (the bridge):
class PayPalAdapter implements PaymentProcessor {

    // Wraps the adaptee via composition
    private final PayPalSDK payPalSDK;

    public PayPalAdapter(PayPalSDK payPalSDK) {
        this.payPalSDK = payPalSDK;
    }

    @Override
    public void processPayment(double amount, String currency) {
        // Translate: PaymentProcessor → PayPalSDK
        PayPalRequest request = new PayPalRequest(amount, currency);
        payPalSDK.makePayment(request); // delegate to adaptee
    }

    @Override
    public void refund(String transactionId) {
        // Translate: transactionId string → PayPalRefundRequest object
        PayPalRefundRequest request = new PayPalRefundRequest(transactionId);
        payPalSDK.initiateRefund(request); // delegate to adaptee
    }
}

//Step 5 — Client code — zero changes to existing system:
class PaymentService {

    private final PaymentProcessor processor;

    // Depends on interface, not implementation — DIP
    public PaymentService(PaymentProcessor processor) {
        this.processor = processor;
    }

    public void checkout(double amount, String currency) {
        processor.processPayment(amount, currency);
    }

    public void processRefund(String transactionId) {
        processor.refund(transactionId);
    }
}

public class Adapter {
    public static void main(String[] args) {
        // Bootstrap
        PaymentProcessor razorpay = new RazorpayProcessor();
        PaymentProcessor paypal   = new PayPalAdapter(new PayPalSDK()); // wrapped!

        PaymentService service = new PaymentService(paypal);
        service.checkout(1500.00, "INR");
        service.processRefund("txn_abc123");

    // Output:
    // PayPal SDK: Payment of 1500.0 INR
    // PayPal SDK: Refund for order txn_abc123
    }
}
