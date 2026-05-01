package DesignPatterns.Important.Structural;

interface Coffee {
    double getCost();
    String getDescription();
}

class Espresso implements Coffee {
    @Override
    public double getCost() {
        return 50.00;
    }

    @Override
    public String getDescription() {
        return "Espresso";
    }
}

class SimpleCoffee implements Coffee {
    @Override
    public double getCost() { return 30.0; }

    @Override
    public String getDescription() { return "Simple Coffee"; }
}

abstract class CoffeeDecorator implements Coffee {
    // Wraps a Coffee component — composition
    protected final Coffee coffee;

    public CoffeeDecorator(Coffee coffee) {
        this.coffee = coffee;
    }

    // Default delegation - subclass override to add behaviour
    @Override
    public double getCost() {
        return coffee.getCost();
    }

    @Override
    public String getDescription() {
        return coffee.getDescription();
    }
}

class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public double getCost() {
        return coffee.getCost() + 10.0; // add milk cost
    }

    @Override
    public String getDescription() {
        return coffee.getDescription() + " , Milk";
    }
}

class SugarDecorator extends CoffeeDecorator {
    public SugarDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public double getCost() {
        return coffee.getCost() + 5.0;
    }

    @Override
    public String getDescription() {
        return coffee.getDescription() + " , Sugar";
    }
}

class WhipDecorator extends CoffeeDecorator {
    public WhipDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public double getCost() {
        return coffee.getCost() + 20.0;
    }

    @Override
    public String getDescription() {
        return coffee.getDescription() + " , Whip";
    }
}

public class Decorator {
    public static void main(String[] args) {
        Coffee order = new Espresso();
        System.out.println(order.getDescription() + " → ₹" + order.getCost());
        // Espresso → ₹50.0

        // Espresso + Milk
        order = new MilkDecorator(new Espresso());
        System.out.println(order.getDescription() + " → ₹" + order.getCost());
        // Espresso, Milk → ₹60.0


        // Espresso + Milk + Sugar + Whip
        order = new WhipDecorator(new SugarDecorator(new MilkDecorator(new Espresso())));
        System.out.println(order.getDescription() + " → ₹" + order.getCost());
        // Espresso, Milk, Sugar, Whipped Cream → ₹85.0

        // Double Milk (wrap same decorator twice!)
        order = new MilkDecorator(new MilkDecorator(new Espresso()));
        System.out.println(order.getDescription() + " → ₹" + order.getCost());
        // Espresso, Milk, Milk → ₹70.0

    }
}
