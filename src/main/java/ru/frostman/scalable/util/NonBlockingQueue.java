package ru.frostman.scalable.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author slukjanov aka Frostman
 */
public class NonBlockingQueue<T> {
    private final Object[] elements;

    private final int size;
    private final Semaphore free;
    private final Semaphore used = new Semaphore(0);
    private final AtomicInteger readPointer = new AtomicInteger(0);
    private final AtomicInteger writePointer = new AtomicInteger(0);

    public NonBlockingQueue() {
        this(1024);
    }

    public NonBlockingQueue(int size) {
        this.size = size;
        elements = new Object[size];
        free = new Semaphore(size);
    }

    public void push(T value) {
        safeAcquire(free);
        elements[getAndIncrementPointer(writePointer)] = value;
        used.release();
    }

    public T pop() {
        safeAcquire(used);
        T value = elements(getAndIncrementPointer(readPointer));
        free.release();

        return value;
    }

    @SuppressWarnings("unchecked")
    private T elements(int idx) {
        return (T) elements[idx];
    }

    private void safeAcquire(Semaphore semaphore) {
        while (true) {
            try {
                semaphore.acquire();
                break;
            } catch (InterruptedException e) {
                // no operations
            }
        }
    }

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private int getAndIncrementPointer(AtomicInteger pointer) {
        int value = -1;

        //todo think about this and think about reentrantlock + primitive int
        while (value < 0) {
            value = pointer.getAndIncrement();
            if (value >= size) {
                COUNTER.incrementAndGet();
                pointer.compareAndSet(value + 1, 0);
                value = -1;
            }
        }

        return value;
    }

    //todo move to tests
    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        NonBlockingQueue<String> queue = new NonBlockingQueue<String>();

        Set<Integer> ids = Collections.synchronizedSet(new LinkedHashSet<Integer>());


        for (int thread = 0; thread < 10; thread++) {
            for (int i = 0; i < 10000000; i++) {
                ids.add(queue.getAndIncrementPointer(queue.readPointer));
            }
        }

        /*
        int id = 0, size = 1024;
        Object lock = new Object();
        for (int thread = 0; thread < 10; thread++) {
            for (int i = 0; i < 100000000; i++) {
                synchronized (lock) {
                    id++;
                    if (id == size) {
                        id = 0;
                    }
                }
            }
        }*/

        for (int i = 0; i < 1024; i++) {
            ids.remove(i);
        }

        long end = System.currentTimeMillis();

        System.out.println(ids);
        System.out.println((end - start) + " ms");
        System.out.println(COUNTER);
    }

}
