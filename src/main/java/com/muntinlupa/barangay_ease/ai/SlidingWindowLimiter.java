package com.muntinlupa.barangay_ease.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory sliding-window limiter. Use a shared store for multi-instance deployments. */
@Component
public class SlidingWindowLimiter {

    private final Map<String, Deque<Long>> requestTimes = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key) {
        return tryAcquire(key, 1, 1_000L);
    }

    public synchronized boolean tryAcquire(String key, int maxRequests, long windowMillis) {
        if (key == null || key.isBlank() || maxRequests < 1 || windowMillis < 1) return false;

        long now = System.currentTimeMillis();
        long cutoff = now - windowMillis;
        Deque<Long> times = requestTimes.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        while (!times.isEmpty() && times.peekFirst() <= cutoff) {
            times.removeFirst();
        }
        if (times.size() >= maxRequests) return false;

        times.addLast(now);
        if (requestTimes.size() > 10_000) {
            requestTimes.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }
        return true;
    }
}
