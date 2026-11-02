package com.movieticket.os;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Unit 6: Producer-Consumer Notification Service using BoundedBuffer<String>,
 * background NotificationWorkerThread (extends Thread), synchronized LinkedList history, and join().
 */
@Component
public class TicketNotificationService {

    private static final int BUFFER_CAPACITY = 64;
    private static final int MAX_HISTORY = 50;

    private final BoundedBuffer<String> buffer;
    private final LinkedList<String> processedHistory = new LinkedList<String>();
    private final NotificationWorkerThread workerThread;
    private int processedCount = 0;

    public TicketNotificationService() {
        this.buffer = new BoundedBuffer<String>(BUFFER_CAPACITY);
        this.workerThread = new NotificationWorkerThread(this.buffer, this);
        this.workerThread.start();
    }

    /**
     * Producer method invoked by HTTP/Socket booking and payment threads.
     */
    public void publishNotification(String eventMessage) {
        if (eventMessage == null) {
            return;
        }
        try {
            buffer.put(eventMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    synchronized void recordProcessedNotification(String entry) {
        processedHistory.addFirst(entry);
        processedCount = processedCount + 1;
        while (processedHistory.size() > MAX_HISTORY) {
            processedHistory.removeLast();
        }
    }

    public synchronized int getProcessedCount() {
        return processedCount;
    }

    public int getPendingBufferSize() {
        return buffer.size();
    }

    public int getBufferCapacity() {
        return buffer.getCapacity();
    }

    public String getWorkerThreadName() {
        return workerThread.getName();
    }

    public String getWorkerThreadState() {
        return workerThread.getState().toString();
    }

    public synchronized List<String> getRecentNotifications(int limit) {
        List<String> snapshot = new ArrayList<String>();
        Iterator<String> iterator = processedHistory.iterator();
        int count = 0;
        while (iterator.hasNext() && count < limit) {
            snapshot.add(iterator.next());
            count++;
        }
        return snapshot;
    }

    /**
     * Gracefully closes the buffer and waits for the worker thread to finish using Thread.join().
     */
    public void shutdownAndJoin() {
        buffer.close();
        workerThread.interrupt();
        try {
            workerThread.join(2000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

