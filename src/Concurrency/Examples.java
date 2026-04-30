package Concurrency;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Examples {
    public static void main(String[] args){
/*        AtomicInteger x = new AtomicInteger(0);

        Thread t1 = new Thread(() -> {
            x.incrementAndGet();
        });

        Thread t2 = new Thread(() -> {
            x.incrementAndGet();
        });

        t1.start();
        t2.start();

        System.out.println(x.get());*/

        Lock lock = new ReentrantLock();
        lock.lock();

        try {
            lock.lock();
            System.out.println("Inside");
        }finally {
            lock.unlock();
        }

        lock.unlock();
    }
}
