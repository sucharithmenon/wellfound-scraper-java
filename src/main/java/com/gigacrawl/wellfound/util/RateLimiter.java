package com.gigacrawl.wellfound.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe rate limiter using token bucket algorithm
 * Optimized for Wellfound's anti-bot measures (1-2 req/sec max)
 */
public class RateLimiter {
    
    private final long intervalNanos;
    private final AtomicLong lastRequestTime;
    
    /**
     * Create rate limiter with specified requests per second
     * @param requestsPerSecond Maximum requests per second (recommended: 1.5 for Wellfound)
     */
    public RateLimiter(double requestsPerSecond) {
        if (requestsPerSecond <= 0) {
            throw new IllegalArgumentException("Requests per second must be positive");
        }
        this.intervalNanos = (long) (1_000_000_000.0 / requestsPerSecond);
        this.lastRequestTime = new AtomicLong(0);
    }
    
    /**
     * Acquire permission to make a request, blocking if necessary
     */
    public void acquire() {
        long currentTime = System.nanoTime();
        long lastTime = lastRequestTime.get();
        
        if (lastTime > 0) {
            long timeSinceLastRequest = currentTime - lastTime;
            long timeToWait = intervalNanos - timeSinceLastRequest;
            
            if (timeToWait > 0) {
                try {
                    Thread.sleep(timeToWait / 1_000_000, (int) (timeToWait % 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Rate limiter interrupted", e);
                }
            }
        }
        
        lastRequestTime.set(System.nanoTime());
    }
    
    /**
     * Try to acquire permission without blocking
     * @return true if permission granted, false if rate limited
     */
    public boolean tryAcquire() {
        long currentTime = System.nanoTime();
        long lastTime = lastRequestTime.get();
        
        if (lastTime > 0) {
            long timeSinceLastRequest = currentTime - lastTime;
            if (timeSinceLastRequest < intervalNanos) {
                return false;
            }
        }
        
        return lastRequestTime.compareAndSet(lastTime, currentTime);
    }
    
    /**
     * Get the configured rate limit
     * @return requests per second
     */
    public double getRate() {
        return 1_000_000_000.0 / intervalNanos;
    }
    
    /**
     * Reset the rate limiter (useful for session resets)
     */
    public void reset() {
        lastRequestTime.set(0);
    }
}