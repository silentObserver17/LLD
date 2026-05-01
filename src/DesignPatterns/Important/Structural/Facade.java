package DesignPatterns.Important.Structural;

class InventoryService {
    public boolean checkStock(int productId) {
        System.out.println("Inventory: Checking stock for product " + productId);
        return true; // assume in stock
    }

    public void reserveStock(int productId) {
        System.out.println("Inventory: Reserving stock for product " + productId);
    }
}

class PaymentServiceFacade {
    public boolean validateCard(String cardNumber) {
        System.out.println("Payment: Validating card " + cardNumber);
        return true;
    }

    public String charge(double amount) {
        System.out.println("Payment: Charging ₹" + amount);
        return "TXN_" + System.currentTimeMillis();
    }

    public void refund(String transactionId) {
        System.out.println("Payment: Refunding txn " + transactionId);
    }
}

class OrderService {
    public int createOrder(int userId, int productId, String transactionId) {
        System.out.println("Order: Creating order for user " + userId);
        return (int)(Math.random() * 10000); // orderId
    }
}

class NotificationService {
    public void sendEmail(int userId, String message) {
        System.out.println("Notification: Email to user " + userId + " → " + message);
    }

    public void sendSMS(int userId, String message) {
        System.out.println("Notification: SMS to user " + userId + " → " + message);
    }
}

class ShippingService {
    public void scheduleDelivery(int orderId) {
        System.out.println("Shipping: Scheduling delivery for order " + orderId);
    }
}

class InvoiceService {
    public void generateInvoice(int orderId) {
        System.out.println("Invoice: Generating invoice for order " + orderId);
    }
}

class OrderFacade {
    private final InventoryService inventoryService;
    private final PaymentServiceFacade paymentService;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final ShippingService shippingService;
    private final InvoiceService invoiceService;

    public OrderFacade() {
        // Facade owns the subsystem instances
        this.inventoryService    = new InventoryService();
        this.paymentService      = new PaymentServiceFacade();
        this.orderService        = new OrderService();
        this.notificationService = new NotificationService();
        this.shippingService     = new ShippingService();
        this.invoiceService      = new InvoiceService();
    }

    // Constructor injection variant — better for testing
    public OrderFacade(
            InventoryService inventory,
            PaymentServiceFacade payment,
            OrderService order,
            NotificationService notification,
            ShippingService shipping,
            InvoiceService invoice
    ) {
        this.inventoryService    = inventory;
        this.paymentService      = payment;
        this.orderService        = order;
        this.notificationService = notification;
        this.shippingService     = shipping;
        this.invoiceService      = invoice;
    }

    // One clean method hides the entire orchestration
    public int placeOrder(int userId, int productId, String cardNumber, double amount) {

        System.out.println("\n── Placing Order ──────────────────");

        // 1. Check stock
        if (!inventoryService.checkStock(productId)) {
            throw new IllegalStateException("Product " + productId + " is out of stock");
        }

        // 2. Validate and charge payment
        if (!paymentService.validateCard(cardNumber)) {
            throw new IllegalStateException("Invalid card");
        }
        String transactionId = paymentService.charge(amount);

        // 3. Reserve stock
        inventoryService.reserveStock(productId);

        // 4. Create order
        int orderId = orderService.createOrder(userId, productId, transactionId);

        // 5. Notifications
        notificationService.sendEmail(userId, "Order #" + orderId + " confirmed!");
        notificationService.sendSMS(userId, "Your order is on the way");

        // 6. Schedule delivery
        shippingService.scheduleDelivery(orderId);

        // 7. Generate invoice
        invoiceService.generateInvoice(orderId);

        System.out.println("── Order Complete: #" + orderId + " ──\n");
        return orderId;
    }

    // Facade can expose other simplified operations too
    public void cancelOrder(int orderId, String transactionId) {
        System.out.println("\n── Cancelling Order #" + orderId + " ──");
        paymentService.refund(transactionId);
        notificationService.sendEmail(0, "Order #" + orderId + " cancelled. Refund initiated.");
        System.out.println("── Cancellation Complete ──\n");
    }
}

class CheckoutController {

    private final OrderFacade orderFacade = new OrderFacade();

    public void checkout(int userId, int productId, String card, double amount) {
        int orderId = orderFacade.placeOrder(userId, productId, card, amount);
        System.out.println("Controller: Done. Order ID = " + orderId);
    }
}


public class Facade {
    public static void main(String[] args){
        // Usage
        CheckoutController controller = new CheckoutController();
        controller.checkout(1, 42, "4111-1111-1111-1111", 1499.0);

        // Output:
        // ── Placing Order ──────────────────
        // Inventory: Checking stock for product 42
        // Payment: Validating card 4111-1111-1111-1111
        // Payment: Charging ₹1499.0
        // Inventory: Reserving stock for product 42
        // Order: Creating order for user 1
        // Notification: Email to user 1 → Order #7823 confirmed!
        // Notification: SMS to user 1 → Your order is on the way
        // Shipping: Scheduling delivery for order 7823
        // Invoice: Generating invoice for order 7823
        // ── Order Complete: #7823 ──
        // Controller: Done. Order ID = 7823

    }
}
