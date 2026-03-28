package com.queuesmart.service;

import com.queuesmart.model.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WaitTimeEstimatorTest {

    private WaitTimeEstimator estimator;

    @BeforeEach
    void setUp() {
        estimator = new WaitTimeEstimator();
    }

    // ---- estimate() ----

    @Test
    void estimate_FirstInQueue_ReturnsZero() {
        int wait = estimator.estimate(1, 10, Service.PriorityLevel.MEDIUM);
        assertEquals(0, wait); // position 1 = no one ahead
    }

    @Test
    void estimate_SecondPosition_MediumPriority() {
        // (2-1) * 10 * 0.8 = 8
        int wait = estimator.estimate(2, 10, Service.PriorityLevel.MEDIUM);
        assertEquals(8, wait);
    }

    @Test
    void estimate_ThirdPosition_LowPriority() {
        // (3-1) * 10 * 1.0 = 20
        int wait = estimator.estimate(3, 10, Service.PriorityLevel.LOW);
        assertEquals(20, wait);
    }

    @Test
    void estimate_HighPriority_ReducesWait() {
        // (3-1) * 10 * 0.6 = 12
        int wait = estimator.estimate(3, 10, Service.PriorityLevel.HIGH);
        assertEquals(12, wait);
    }

    @Test
    void estimate_NullPriority_DefaultsToLow() {
        // (3-1) * 10 * 1.0 = 20
        int wait = estimator.estimate(3, 10, null);
        assertEquals(20, wait);
    }

    @Test
    void estimate_ZeroOrNegativePosition_ReturnsZero() {
        assertEquals(0, estimator.estimate(0, 10, Service.PriorityLevel.MEDIUM));
        assertEquals(0, estimator.estimate(-1, 10, Service.PriorityLevel.HIGH));
    }

    @Test
    void estimate_LargeQueue_CalculatesCorrectly() {
        // position=10, duration=5, LOW: (10-1)*5*1.0 = 45
        int wait = estimator.estimate(10, 5, Service.PriorityLevel.LOW);
        assertEquals(45, wait);
    }

    // ---- estimateForNewUser() ----

    @Test
    void estimateForNewUser_EmptyQueue_ReturnsZero() {
        assertEquals(0, estimator.estimateForNewUser(0, 15));
    }

    @Test
    void estimateForNewUser_FiveInQueue_ReturnsCorrectWait() {
        // 5 * 15 = 75
        assertEquals(75, estimator.estimateForNewUser(5, 15));
    }

    @Test
    void estimateForNewUser_OneInQueue_ReturnsDuration() {
        assertEquals(10, estimator.estimateForNewUser(1, 10));
    }
}
