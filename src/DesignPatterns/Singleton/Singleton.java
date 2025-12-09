package DesignPatterns.Singleton;

/*
* Ensure class has only one instance and provide global point of access to that instance.
* Example: We are building an application, and we want one shared object shared throughout
* the application, the singleton design patterns restricts object creation and guarantees
* that all parts of the application uses same object.
*
* Real-world use cases:
* Database connection pool
* Logger
* Configuration manager
* Runtime / SecurityManager in Java
* */

//In Eager Loading, the Singleton instance is created as soon as the class is loaded, regardless of whether it's ever used. Let's understand this with a real-life analogy.

import java.util.Properties;

// 1. EAGER WITH PUBLIC FIELD: Too exposed, no encapsulation.
class SingletonFirstExample{
    public static final SingletonFirstExample INSTANCE = new SingletonFirstExample();
    private SingletonFirstExample(){}
}

// 2. Classic Eager Initialization (Acceptable) Simple and thread safe. Instance created at class loading even though it's never used.
class SingletonSecondExample{
    private static final SingletonSecondExample INSTANCE = new SingletonSecondExample();

    private SingletonSecondExample(){
        System.out.println("Second SingleTon Created");
    }

    public static SingletonSecondExample getInstance(){
        return INSTANCE;
    }
}

//3. Lazy + Synchronized (Expensive) => Thread-safe but synchronized on every call → terrible performance
class SingletonThirdExample{
    private static SingletonThirdExample instance;

    private SingletonThirdExample(){}

    public static synchronized SingletonThirdExample getInstance(){
        if(instance == null){
            instance = new SingletonThirdExample();
        }
        return instance;
    }
}

//4–5. Double-Checked Locking (The “Classic” Way)
/*
- Lazy (created only when getInstance() is called)
- Thread-safe (thanks to class loading)
- No synchronization needed
- Used in many libraries
*/

//Because of multi-threading
//Imagine two threads (Thread A and Thread B) call getInstance() at the same time when instance is still null.
class SingletonClassic{
    private static volatile SingletonClassic instance;

    public SingletonClassic(){}

    public static SingletonClassic getInstance(){
        if(instance == null){
            synchronized (SingletonClassic.class){
                if(instance == null){
                    instance = new SingletonClassic();
                }
            }
        }

        return instance;
    }
}

// 6. Bill Pugh Singleton
// This is a highly efficient way to implement the Singleton pattern. It uses a static inner helper class to hold the Singleton instance. The instance is created only when the inner class is loaded, which happens only when getInstance() is called for the first time.
class SingletonBill{
    private SingletonBill(){}

    private static class Holder{
        private static final SingletonBill INSTANCE = new SingletonBill();
    }

    public static SingletonBill getInstance(){
        return Holder.INSTANCE;
    }
}


//7. ENUM SINGLETON: The Winner
// Implementation   Thread-Safe  Lazy      Serialization   SafeReflection   SafeVerdict
// Enum             Yes          Yes       Yes             Yes              PERFECT
public enum Singleton {
    INSTANCE;

    public static Singleton getInstance(){
        return INSTANCE;
    }
}

// another example:
enum ConfigManager{
    INSTANCE;

    private final Properties props = new Properties();

    ConfigManager(){
        // load data from file.
    }

    public String get(String key){ return "string"; }
}