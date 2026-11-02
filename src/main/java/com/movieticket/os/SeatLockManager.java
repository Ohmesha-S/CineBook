package com.movieticket.os;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unit 6: Java for Operating System - Deadlock Prevention via Global Lock Ordering.
 *
 * If Thread-A requests seats [105, 102] and Thread-B simultaneously requests seats [102, 105]
 * for the same show, acquiring monitor locks in request order causes a Circular Wait Deadlock.
 * SeatLockManager breaks the Circular Wait Coffman condition by deduplicating and sorting all
 * requested seat IDs in strict ascending order (Collections.sort) before acquiring nested
 * synchronized monitor locks.
 */
@Component
public class SeatLockManager {

    private final Map<String, Object> seatMonitors = new HashMap<String, Object>();
    private final BookingMetricsCounter metricsCounter;

    public SeatLockManager(BookingMetricsCounter metricsCounter) {
        this.metricsCounter = metricsCounter;
    }

    public <T> T executeWithOrderedSeatLocks(Long showId, List<Long> seatIds, SeatLockTask<T> task) {
        if (showId == null || seatIds == null || seatIds.isEmpty()) {
            return task.execute();
        }

        Set<Long> uniqueIds = new HashSet<Long>();
        for (Long seatId : seatIds) {
            if (seatId != null) {
                uniqueIds.add(seatId);
            }
        }

        List<Long> orderedSeatIds = new ArrayList<Long>(uniqueIds);
        Collections.sort(orderedSeatIds);

        if (metricsCounter != null) {
            metricsCounter.incrementOrderedLockAcquisitions();
        }

        return acquireLockRecursively(showId, orderedSeatIds, 0, task);
    }

    private <T> T acquireLockRecursively(Long showId, List<Long> orderedSeatIds, int index, SeatLockTask<T> task) {
        if (index >= orderedSeatIds.size()) {
            return task.execute();
        }
        Object monitor = getOrCreateMonitor(showId, orderedSeatIds.get(index));
        synchronized (monitor) {
            return acquireLockRecursively(showId, orderedSeatIds, index + 1, task);
        }
    }

    private synchronized Object getOrCreateMonitor(Long showId, Long seatId) {
        String key = showId + ":" + seatId;
        Object monitor = seatMonitors.get(key);
        if (monitor == null) {
            monitor = new Object();
            seatMonitors.put(key, monitor);
        }
        return monitor;
    }
}

