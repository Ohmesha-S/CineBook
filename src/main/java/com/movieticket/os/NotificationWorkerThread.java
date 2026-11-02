package com.movieticket.os;

import java.time.LocalDateTime;

/**
 * Unit 6: Java for Operating System - Thread creation by extending Thread,
 * Inter-Thread Communication consumer loop, and graceful interruption.
 */
public class NotificationWorkerThread extends Thread {

    private final BoundedBuffer<String> buffer;
    private final TicketNotificationService notificationService;

    public NotificationWorkerThread(BoundedBuffer<String> buffer, TicketNotificationService notificationService) {
        super("CineBook-NotificationConsumer");
        this.buffer = buffer;
        this.notificationService = notificationService;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                String message = buffer.take();
                if (message == null) {
                    break;
                }
                String processedEntry = "[" + LocalDateTime.now() + "] [" + getName() + "] " + message;
                notificationService.recordProcessedNotification(processedEntry);
            } catch (InterruptedException e) {
                interrupt();
                break;
            }
        }
    }
}

