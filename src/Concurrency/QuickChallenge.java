package Concurrency;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class BlockingQueue<T> {
    private Queue<T> queue = new LinkedList<>();
    private int capacity;

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void enqueue(T item) throws InterruptedException {
        while(queue.size() == capacity){
            wait();
        }
        queue.offer(item);
        notifyAll();
    }

    public synchronized T dequeue() throws InterruptedException {
        while(queue.isEmpty()){
            wait();
        }

        T item = queue.poll();
        notifyAll();
        return item;
    }
}

class SimpleThreadPool {
    private final Queue<Runnable> workQueue = new LinkedList<>();
    private final List<Worker> workers = new ArrayList<>();
    private final int capacity = 100; // internal queue limit

    private boolean isShutdown = false;

    public SimpleThreadPool(int workerCount) {
        for(int i = 0; i < workerCount; i++){
            Worker worker = new Worker("worker-" + i);
            workers.add(worker);
            worker.start();
        }
    }

    public synchronized void submit(Runnable task) throws InterruptedException {
        if (isShutdown){
            throw new IllegalStateException("Cannot submit task: pool is already shut down");
        }

        while(workQueue.size() == capacity){
            wait();
        }
        workQueue.offer(task);
        notifyAll();
    }

    public synchronized void shutdown(){
        isShutdown = true;
        notifyAll();
    }

    public void awaitTermination() throws InterruptedException {
        for (Worker worker : workers) {
            worker.join(); // wait for each worker thread to fully exit
        }
    }


    class Worker extends Thread{
        Worker(String name) {
            super(name);
        }

        @Override
        public void run() {
            while(true) {
                Runnable task = null;

                synchronized (SimpleThreadPool.this) {
                    while (workQueue.isEmpty() && !isShutdown) {
                        try {
                            SimpleThreadPool.this.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    if (isShutdown && workQueue.isEmpty()) {
                        return;
                    }

                    task = workQueue.poll();
                    SimpleThreadPool.this.notifyAll();
                }

                if(task != null) {
                    task.run();
                }
            }
        }
    }
}

public class QuickChallenge {
    static boolean ready = false;
    static int number = 0;

    public static void main(String[] args) throws InterruptedException {
        new Thread(() -> {
            number = 42;
            ready = true;
        }).start();

        new Thread(() -> {
            while(!ready){}
            System.out.println("Thread " + number + " is ready");
        }).start();


        // Blocking Queue
     /*   BlockingQueue<Integer> bq = new BlockingQueue<>(5);
        new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    bq.enqueue(i);
                    System.out.println("Producer " + i + " enqueued");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                while(true) {
                    int item = bq.dequeue();
                    System.out.println("Consumer " + item + " dequeued");
                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }).start();*/

        SimpleThreadPool simpleThreadPool = new SimpleThreadPool(3);

        // Submit 10 Tasks.
        for(int i = 1; i <= 10; i++){
            final int taskId = i;
            simpleThreadPool.submit(() -> {
                String name =  Thread.currentThread().getName();
                System.out.println("Task " + taskId + " started by " + name);
                // simulate Work
                try{
                    Thread.sleep(500);
                } catch (Exception e) {
                    System.out.println(name + " interrupted.");
                }
            });
        }

        // 3. Initiate shutdown
        System.out.println("--- Initiating Shutdown ---");
        simpleThreadPool.shutdown();
        simpleThreadPool.awaitTermination(); // ← add this!
        System.out.println("All tasks done.");

        AtomicInteger x = new AtomicInteger(0);

        Thread t1 = new Thread(() -> {
            x.incrementAndGet();
        });

        Thread t2 = new Thread(() -> {
            x.incrementAndGet();
        });

        t1.start();
        t2.start();

        System.out.println(x.get());
    }
}
