package com.queuesmart.service;

import com.queuesmart.model.Service;
import org.springframework.stereotype.Component;

/**
 * Rule-based wait time estimator.
 * Formula: estimatedWait = position × service.expectedDurationMinutes
 * A priority multiplier is applied so HIGH priority users wait less.
 */
@Component
public class WaitTimeEstimator {

    /**
     * @param position                 1-based position in queue
     * @param expectedDurationMinutes  service's expected duration per customer
     * @param priority                 user's priority level
     * @return estimated wait in minutes
     */
    public int estimate(int position, int expectedDurationMinutes, Service.PriorityLevel priority) {
        if (position <= 0) return 0;
        // Base wait: everyone ahead × duration
        int baseWait = (position - 1) * expectedDurationMinutes;
        // Priority reduces wait by bumping the multiplier down
        double multiplier = priorityMultiplier(priority);
        return (int) Math.ceil(baseWait * multiplier);
    }

    /**
     * Estimate wait for a new user who would join at the end.
     */
    public int estimateForNewUser(int currentQueueSize, int expectedDurationMinutes) {
        return currentQueueSize * expectedDurationMinutes;
    }

    private double priorityMultiplier(Service.PriorityLevel priority) {
        if (priority == null) return 1.0;
        return switch (priority) {
            case HIGH   -> 0.6;  // HIGH priority users skip ~40% of wait
            case MEDIUM -> 0.8;
            case LOW    -> 1.0;
        };
    }


    /**
     * Estimates wait time with a custom multiplier override.
     * Useful for services that have variable throughput at different times.
     */
    public int estimateWithMultiplier(int position, int durationMinutes, double customMultiplier) {
        if (position <= 1) return 0;
        double base = (double)(position - 1) * durationMinutes;
        return (int) Math.ceil(base * customMultiplier);
    }

    /**
     * Returns a human-readable wait time string, e.g. "~15 min" or "Less than 1 min".
     */
    public String getWaitLabel(int position, int durationMinutes,
            com.queuesmart.model.Service.PriorityLevel priority) {
        int minutes = estimate(position, durationMinutes, priority);
        if (minutes == 0) return "You are next";
        if (minutes < 1) return "Less than 1 min";
        return "~" + minutes + " min";
    }

}
