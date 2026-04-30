package Concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

class Counter {
    private int count = 0;

    public void increment() {
        count++;
    }

    public int get() {
        return count;
    }
}

class CounterSynchronized {
    private int count = 0;
    public synchronized void increment() {
        count++;
    }
    public int get() {
        return count;
    }
}

class CounterAtomic{
    private AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        count.incrementAndGet();
    }
    public int get() {
        return count.get();
    }
}

class CounterReentrant{
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();

    public void increment() {
        lock.lock();
        try {
            count++;
        }finally {
            lock.unlock();
        }
    }

    public int get() {
        return count;
    }
}

public class BasicConcurrency {
    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();
        CounterSynchronized cs = new CounterSynchronized();
        CounterAtomic ca = new CounterAtomic();
        CounterReentrant cr = new CounterReentrant();

        int threadCount = 100;
        int incrementsPerThread = 100;

        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for(int i = 0; i < threadCount; i++){
            new Thread(()->{
                try{
                    for(int j = 0; j < incrementsPerThread; j++){
                        counter.increment();
                        cs.increment();
                        ca.increment();
                        cr.increment();
                    }
                }
                finally {
                    countDownLatch.countDown();
                }
            }).start();
        }

        countDownLatch.await();
        System.out.println("Normal Final count: " + counter.get());
        System.out.println("Synchronized Final count: " + cs.get());
        System.out.println("Atomic Final count: " + ca.get());
        System.out.println("Reentrant Final count: " + cr.get());
        System.out.println("Expected count: " + (threadCount * incrementsPerThread));
    }
}
