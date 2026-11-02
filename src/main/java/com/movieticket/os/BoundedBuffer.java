package com.movieticket.os;

import java.util.LinkedList;

/**
 * Unit 2 & Unit 6: Generic Bounded Buffer using LinkedList<T> and Inter-Thread Communication
 * (synchronized, wait(), and notifyAll()) for Producer-Consumer thread coordination.
 */
public class BoundedBuffer<T> {

    private final LinkedList<T> queue = new LinkedList<T>();
    private final int capacity;
    private boolean open = true;

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Buffer capacity must be positive");
        }
        this.capacity = capacity;
    }

    /**
     * Producer puts an item into the bounded buffer, waiting while the buffer is full.
     */
    public synchronized void put(T item) throws InterruptedException {
        while (queue.size() >= capacity && open) {
            wait();
        }
        if (!open) {
            return;
        }
        queue.addLast(item);
        notifyAll();
    }

    /**
     * Consumer takes an item from the bounded buffer, waiting while the buffer is empty.
     */
    public synchronized T take() throws InterruptedException {
        while (queue.isEmpty() && open) {
            wait();
        }
        if (queue.isEmpty()) {
            return null;
        }
        T item = queue.removeFirst();
        notifyAll();
        return item;
    }

    public synchronized int size() {
        return queue.size();
    }

    public int getCapacity() {
        return capacity;
    }

    public synchronized boolean isOpen() {
        return open;
    }

    public synchronized void close() {
        this.open = false;
        notifyAll();
    }
}

