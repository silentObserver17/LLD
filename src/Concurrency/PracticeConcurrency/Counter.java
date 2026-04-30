package Concurrency.PracticeConcurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

class NormalCounter{
    private int count = 0;
    public void increment() {
        count++;
    }
    public int get() {
        return count;
    }
}

class SynchronizedCounter{
    private int count = 0;

    public synchronized void increment() {
        count++;
    }

    public synchronized int get() {
        return count;
    }
}

class AtomicIntegerCounter{
    private AtomicInteger count = new AtomicInteger(0);
    public void increment() {
        count.incrementAndGet();
    }

    public int get() {
        return count.get();
    }
}

class ReentrantLockCounter{
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
        lock.lock();
        try{
            return count;
        }finally {
            lock.unlock();
        }
    }
}

class Counter {
    public static void main(String[] args) throws InterruptedException {
        NormalCounter nc = new NormalCounter();
        SynchronizedCounter sc = new SynchronizedCounter();
        AtomicIntegerCounter ac = new AtomicIntegerCounter();
        ReentrantLockCounter rl = new ReentrantLockCounter();

        int threadCount = 100;
        int incrementsPerThread = 100;

        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for(int i = 0; i < threadCount; i++){
            new Thread(() -> {
                try{
                    for(int j = 0; j < incrementsPerThread; j++){
                        nc.increment();
                        sc.increment();
                        ac.increment();
                        rl.increment();
                    }
                }
                finally {
                    countDownLatch.countDown();
                }
            }).start();
        }

        countDownLatch.await();
        System.out.println("Normal Final count: " + nc.get());
        System.out.println("Synchronized Final count: " + sc.get());
        System.out.println("Atomic Final count: " + ac.get());
        System.out.println("Reentrant Lock Final count: " + rl.get());
    }
}

