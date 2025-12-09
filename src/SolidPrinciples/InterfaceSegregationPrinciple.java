package SolidPrinciples;

/*
* Do not force class to depend on methods/interfaces they do not use.
* In other words, Prefer small, specific interfaces over large, fat interface.
* an example of it is worker, A worker needs to work, eat, sleep and take break.
* but now we have introduced a new robot worker, robots don't need to eat or sleep. so
* the robot class is forced to implement methods that it does not need.
* */

interface Workable{
    void work();
}

interface Eatable{
    void eat();
}

interface Sleepable{
    void sleep();
}

interface Breakable{
    void takeBreak();
}

class HumanWorker implements Workable, Eatable, Sleepable, Breakable {
    @Override public void work()      { System.out.println("Human working"); }
    @Override public void eat()        { System.out.println("Human eating"); }
    @Override public void sleep()      { System.out.println("Human sleeping"); }
    @Override public void takeBreak()  { System.out.println("Human taking break"); }
}

class RobotWorker implements Workable{
    @Override
    public void work() {
        System.out.println("Robot working 24/7");
    }
    // Here No eat(); No sleep(); or takeBreak();
}

public class InterfaceSegregationPrinciple {

}
