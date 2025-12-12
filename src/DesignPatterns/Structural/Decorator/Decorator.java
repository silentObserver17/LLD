package DesignPatterns.Structural.Decorator;

/*
* Decorator is a structural Design Pattern that lets us attach new behaviours to objects dynamically by placing them in special wrapper objects(decorators).'
* Its proper way to extend functionality without subclassing.
* example: plain pizza -> add cheese -> add olives -> add extra cheese -> add pepperoni.
* why do we need decorators why not inheritance? => because inheritance fails when we have so many optional features.
* */


interface Pizza{
    String getDescription();
    double getCost();
}

// Concrete Component – The plain base pizzas
class Margherita implements Pizza {
    @Override
    public String getDescription() {
        return "Margherita";
    }

    @Override
    public double getCost() {
        return 199.00;
    }
}

class FarmHouse implements Pizza {
    @Override
    public String getDescription() {
        return "Farmhouse";
    }

    @Override
    public double getCost() {
        return 299.00;
    }
}

// Abstract Decorator
abstract class ToppingDecorator implements Pizza{
    protected final Pizza pizza; // the type of pizza we are wrapping

    public ToppingDecorator(Pizza pizza){
        this.pizza = pizza;
    }

    @Override
    public String getDescription(){
        return pizza.getDescription();
    }

    @Override
    public double getCost(){
        return pizza.getCost();
    }
}

// Concrete Decorators.
class Cheese extends ToppingDecorator{
    public Cheese(Pizza pizza) {
        super(pizza);
    }

    @Override
    public String getDescription() {
        return pizza.getDescription() + ", Extra Cheese";
    }

    @Override
    public double getCost() {
        return pizza.getCost() + 60.00;
    }
}

class Olives extends ToppingDecorator {
    public Olives(Pizza pizza) {
        super(pizza);
    }

    @Override
    public String getDescription() {
        return pizza.getDescription() + ", Olives";
    }

    @Override
    public double getCost() {
        return pizza.getCost() + 40.00;
    }
}

public class Decorator {
    static void print(Pizza pizza) {
        System.out.printf("%-50s → ₹%.2f%n",
                pizza.getDescription(), pizza.getCost());
        System.out.println("─".repeat(60));
    }

    public static void main(String[] main){
        Pizza p1 = new Margherita();
        print(p1);

        Pizza p2 = new Olives(
                new Cheese(
                        new Cheese(
                                new FarmHouse()
                        )
                )
        );

        print(p2);

        Pizza p3 = new Margherita();
        p3 = new Olives(p3);
        p3 = new Cheese(p3);
        print(p3);
    }
}
