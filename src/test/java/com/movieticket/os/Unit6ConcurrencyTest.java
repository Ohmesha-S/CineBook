package com.movieticket.os;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit 6: Java for Operating System - Multithreading, Thread.join(), Synchronized Race-Condition Counter,
 * Inter-Thread Communication (wait/notifyAll BoundedBuffer), and Deadlock Prevention (SeatLockManager).
 */
public class Unit6ConcurrencyTest {

    @Test
    @DisplayName("Unit 6.3 & 6.4: Synchronized BookingMetricsCounter prevents race conditions across concurrent threads")
    void testSynchronizedCounterPreventsRaceCondition() throws InterruptedException {
        final BookingMetricsCounter counter = new BookingMetricsCounter();
        final int threadCount = 10;
        final int incrementsPerThread = 200;

        Thread[] workers = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            workers[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < incrementsPerThread; i++) {
                        counter.incrementBookingAttempts();
                    }
                }
            }, "CounterWorker-" + t);
            workers[t].start();
        }

        for (int t = 0; t < threadCount; t++) {
            workers[t].join();
        }

        assertEquals(threadCount * incrementsPerThread, counter.getBookingAttempts());
    }

    @Test
    @DisplayName("Unit 6.6: BoundedBuffer wait() and notifyAll() coordinates Producer-Consumer threads")
    void testProducerConsumerBoundedBuffer() throws InterruptedException {
        final BoundedBuffer<String> buffer = new BoundedBuffer<String>(3);
        final List<String> consumed = new ArrayList<String>();
        final int totalMessages = 15;

        Thread producer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 1; i <= totalMessages; i++) {
                        buffer.put("TICKET-" + i);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "ProducerThread");

        Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 1; i <= totalMessages; i++) {
                        String item = buffer.take();
                        if (item != null) {
                            consumed.add(item);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "ConsumerThread");

        producer.start();
        consumer.start();

        producer.join(3000L);
        consumer.join(3000L);

        assertEquals(totalMessages, consumed.size());
        assertEquals("TICKET-1", consumed.get(0));
        assertEquals("TICKET-15", consumed.get(totalMessages - 1));
    }

    @Test
    @DisplayName("Unit 6.7: SeatLockManager prevents Circular Wait Deadlock when threads lock seats in opposite order")
    void testSeatLockManagerPreventsOppositeOrderDeadlock() throws InterruptedException {
        final BookingMetricsCounter counter = new BookingMetricsCounter();
        final SeatLockManager lockManager = new SeatLockManager(counter);
        final Long showId = Long.valueOf(7L);

        final List<Long> reverseOrderSeats = Arrays.asList(Long.valueOf(105L), Long.valueOf(102L), Long.valueOf(101L));
        final List<Long> forwardOrderSeats = Arrays.asList(Long.valueOf(101L), Long.valueOf(102L), Long.valueOf(105L));

        Thread threadA = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    lockManager.executeWithOrderedSeatLocks(showId, reverseOrderSeats, new SeatLockTask<Boolean>() {
                        @Override
                        public Boolean execute() {
                            counter.incrementConfirmedBookings();
                            return Boolean.TRUE;
                        }
                    });
                }
            }
        }, "SeatBookingThread-A");

        Thread threadB = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    lockManager.executeWithOrderedSeatLocks(showId, forwardOrderSeats, new SeatLockTask<Boolean>() {
                        @Override
                        public Boolean execute() {
                            counter.incrementConfirmedBookings();
                            return Boolean.TRUE;
                        }
                    });
                }
            }
        }, "SeatBookingThread-B");

        threadA.start();
        threadB.start();

        threadA.join(3000L);
        threadB.join(3000L);

        assertFalse(threadA.isAlive(), "Thread A should complete without deadlocking");
        assertFalse(threadB.isAlive(), "Thread B should complete without deadlocking");
        assertEquals(200, counter.getConfirmedBookings());
        assertEquals(200, counter.getOrderedLockAcquisitions());
    }
}

