package DesignPatterns.Important.Creational;

class SingletonEager{
    private static final SingletonEager INSTANCE = new SingletonEager();

    private SingletonEager(){}

    public static SingletonEager getSingletonInstaince(){
        return INSTANCE;
    }
}

class SingletonDoubleLazy{
    private static volatile SingletonDoubleLazy INSTANCE;

    private SingletonDoubleLazy(){}

    public static SingletonDoubleLazy getSingletonInstance(){
        if(INSTANCE == null){
            synchronized (SingletonDoubleLazy.class){
                if(INSTANCE == null){
                    INSTANCE = new SingletonDoubleLazy();
                }
            }
        }

        return INSTANCE;
    }
}

class SingletonBillPugh{
    public SingletonBillPugh(){}

    private static class Holder{
        static final  SingletonBillPugh INSTANCE = new SingletonBillPugh();
    }

    public static SingletonBillPugh getSingletonInstance(){
        return Holder.INSTANCE;
    }
}

enum SingletonEnum{
    INSTANCE;

    public static SingletonEnum getInstance(){
        return INSTANCE;
    }
}

public class Singleton {
}
