package DesignPatterns.Structural.Proxy;
/*
* Proxy is a structural design pattern that provides  a surrogate or placeholder for another object control access to it.
* Think of it as a “bodyguard”, “gatekeeper”, or “smart reference”.
* Client thinks its talking to the real object -> but it's actually talking to the proxy.
* */
// Type of proxy:
//1. Virtual Proxy -> Real objects are expensive to create -> Lazy load 50mb image only when displayed
// 2. Remote Proxy -> Real object lives on another server -> REST/grpc client stub
// 3. Protection Proxy -> Need to check permissions before access -> Admin only methods
// 4. Cache Proxy -> Avoid repeated expensive work -> Cache database or API results.

// Payment Gateway with all 4 proxy types
//1. The common interface.
record PaymentResult(boolean success, String transactionId, String message){}
interface PaymentGateway{
    PaymentResult pay(double amount, String orderId);
}

// 2. The Real implementation(expensive)
class RazorPayGateway implements PaymentGateway{
    public RazorPayGateway(){
        System.out.println("   [Real] Connecting to Razorpay sandbox... (expensive init)");
        try { Thread.sleep(2000); } catch (Exception e) {} // simulate slow init
    }

    @Override
    public PaymentResult pay(double amount, String orderId) {
        System.out.println("   [Real] Actually calling Razorpay API for ₹" + amount);
        return new PaymentResult(true, "rzp_real_" + System.nanoTime(), "Success");
    }
}

// A Virtual Proxy only creates object when needed
class LazyPaymentProxy implements PaymentGateway{
    private RazorPayGateway realGateway; // null initially

    @Override
    public PaymentResult pay(double amount, String orderId) {
        if (realGateway == null) {
            System.out.println("LazyProxy: First call → creating real gateway now");
            realGateway = new RazorPayGateway();
        }
        return realGateway.pay(amount, orderId);
    }
}

// B. Cache Proxy – avoids calling the real gateway twice
class CachedPaymentProxy implements PaymentGateway{
    private final PaymentGateway real;
    private PaymentResult cachedResult;
    private String lastOrderId;

    public CachedPaymentProxy(PaymentGateway real) {
        this.real = real;
    }

    @Override
    public synchronized PaymentResult pay(double amount, String orderId) {
        if(orderId.equals(lastOrderId) && cachedResult != null){
            System.out.println("CachedProxy: HIT! Returning cached result");
            return cachedResult;
        }
        System.out.println("CacheProxy: MISS -> calling real gateway");
        cachedResult = real.pay(amount, orderId);
        lastOrderId = orderId;
        return cachedResult;
    }
}

// C. Protection Proxy – only admin can do refund
class AdminProtectionProxy implements PaymentGateway{
    private final PaymentGateway real;
    private final boolean isAdmin;

    public AdminProtectionProxy(PaymentGateway real, boolean isAdmin) {
        this.real = real;
        this.isAdmin = isAdmin;
    }

    @Override
    public PaymentResult pay(double amount, String orderId) {
        if(amount < 0 && !isAdmin){
            return new PaymentResult(false, null, "Only Admin can refund");
        }
        return real.pay(amount, orderId);
    }
}

// D. Logging / Monitoring Proxy (most common in real apps)
class LoggingProxy implements PaymentGateway{
    private final PaymentGateway real;

    LoggingProxy(PaymentGateway real) {
        this.real = real;
    }

    @Override
    public PaymentResult pay(double amount, String orderId) {
        long start = System.nanoTime();
        PaymentResult result = real.pay(amount, orderId);
        long duration = System.nanoTime() - start;
        System.out.printf("LoggingProxy: %.2f ₹ | %s | %d ms%n",
                amount, result.success() ? "OK" : "FAIL", duration / 1_000_000);
        return result;
    }
}

public class Proxy {
    public static void main(String[] args){
        PaymentGateway real = new RazorPayGateway(); // Expensive

        System.out.println("1. Virtual (Lazy) Proxy");
        PaymentGateway lazy = new LazyPaymentProxy();
        lazy.pay(999, "ORD1");

        System.out.println("\n2. Caching Proxy");
        PaymentGateway cached = new CachedPaymentProxy(real);
        cached.pay(1999, "ORD999");
        cached.pay(1999, "ORD999");   // cache hit!

        System.out.println("\n3. PROTECTION PROXY");
        PaymentGateway userProxy = new AdminProtectionProxy(real, false);
        PaymentGateway adminProxy = new AdminProtectionProxy(real, true);
        userProxy.pay(-500, "REFUND1");   // blocked
        adminProxy.pay(-500, "REFUND2");  // allowed

        System.out.println("\n4. LOGGING PROXY (stack them!)");
        PaymentGateway monitored = new LoggingProxy(
                new CachedPaymentProxy(
                        new LazyPaymentProxy()
                )
        );

        monitored.pay(2999, "ORD100");
        monitored.pay(2999, "ORD100");   // cache hit + logged
    }
}
