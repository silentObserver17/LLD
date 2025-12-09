package SolidPrinciples;

/*
* If S is a subtype of T, then objects of T may be replaced with objects of S without altering
* the correctness of the program.
* In simple words any subclass should be substitutable for its parent class without
* breaking the functionality
* */

// A classic example of violation of LSP is Square inheriting Rectangle.
class Rectangle {
    protected int width;
    protected int height;

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getArea() {
        return width * height;
    }
}

class Square extends Rectangle {
    @Override
    public void setWidth(int width) {
        this.width = width;
        this.height = width;  // Square forces height = width
    }

    @Override
    public void setHeight(int height) {
        this.width = height;
        this.height = height; // Square forces width = height
    }
}

// LSP Safe Example.
abstract class Payment{
    public abstract void processPayment(double amount);
    public void sendReceipt(){
        System.out.println("Receipt sent via Email");
    }
}

class CreditCardLSPPayment extends Payment{
    @Override
    public void processPayment(double amount) {
        System.out.println("Charged $" + amount + " to credit card");
    }
}

class CashLSPPayment extends Payment{
    @Override
    public void processPayment(double amount) {
        System.out.println("Received $" + amount + " in cash");
    }

    @Override
    public void sendReceipt() {
        System.out.println("Printed paper receipt");  // Different but valid
    }
}

public class LiskovSubstitutionPrinciple {
    public static void makePayment(Payment payment, double amount){
        payment.processPayment(amount);
        payment.sendReceipt();
    }

    public static void main(String[] args){
        Rectangle rect  = new Rectangle();
        rect.setHeight(5);
        rect.setWidth(4);

        System.out.println(rect.getArea());

        Rectangle square = new Square();
        square.setHeight(5);
        square.setWidth(4);

        System.out.println(square.getArea()); // prints 16 -> BUG! violation of LSP.

        Payment credit = new CreditCardLSPPayment();
        Payment cash = new CashLSPPayment();

        makePayment(credit, 100);
        makePayment(cash, 200);
    }
}
