package com.komatsu.ahs.generator;

import com.komatsu.ahs.domain.model.VehicleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Vehicle Simulator
 * 
 * Simulates the lifecycle and state transitions of an autonomous haul truck.
 * Manages realistic state transitions: IDLE -> ROUTING -> LOADING -> HAULING -> DUMPING -> ROUTING -> IDLE
 */
public class VehicleSimulator {
    
    private static final Logger LOG = LoggerFactory.getLogger(VehicleSimulator.class);
    
    private final String vehicleId;
    private VehicleStatus currentStatus;
    private int cycleCount;
    private long stateStartTime;
    
    // State duration ranges (in seconds)
    // Realistic haul cycle durations for autonomous mining trucks
    private static final int MIN_ROUTING_TIME = 20;
    private static final int MAX_ROUTING_TIME = 45;
    private static final int MIN_LOADING_TIME = 15;
    private static final int MAX_LOADING_TIME = 30;
    private static final int MIN_HAULING_TIME = 30;
    private static final int MAX_HAULING_TIME = 60;
    private static final int MIN_DUMPING_TIME = 10;
    private static final int MAX_DUMPING_TIME = 20;
    private static final int MIN_IDLE_TIME = 5;
    private static final int MAX_IDLE_TIME = 15;
    
    private int stateDuration;
    private long accumulatedSeconds;
    
    public VehicleSimulator(String vehicleId) {
        this.vehicleId = vehicleId;
        this.currentStatus = VehicleStatus.IDLE;
        this.cycleCount = 0;
        this.stateStartTime = System.currentTimeMillis();
        this.stateDuration = randomInt(MIN_IDLE_TIME, MAX_IDLE_TIME);
        this.accumulatedSeconds = 0;
    }
    
    /**
     * Update vehicle state based on elapsed time
     * Returns true if state changed
     */
    public boolean updateState() {
        // Use a deterministic virtual time step: one second per update call
        accumulatedSeconds += 1;
        if (accumulatedSeconds >= stateDuration) {
            transitionToNextState();
            // Reset virtual time counter after each transition
            accumulatedSeconds = 0;
            return true;
        }
        return false;
    }
    
    /**
     * Transition to the next logical state in the haul cycle
     */
    private void transitionToNextState() {
        VehicleStatus previousStatus = currentStatus;
        
        switch (currentStatus) {
            case IDLE:
                currentStatus = VehicleStatus.ROUTING;
                stateDuration = randomInt(MIN_ROUTING_TIME, MAX_ROUTING_TIME);
                break;
                
            case ROUTING:
                // Deterministic transition to LOADING for stable tests
                currentStatus = VehicleStatus.LOADING;
                stateDuration = randomInt(MIN_LOADING_TIME, MAX_LOADING_TIME);
                break;
                
            case LOADING:
                currentStatus = VehicleStatus.HAULING;
                stateDuration = randomInt(MIN_HAULING_TIME, MAX_HAULING_TIME);
                break;
                
            case HAULING:
                currentStatus = VehicleStatus.DUMPING;
                stateDuration = randomInt(MIN_DUMPING_TIME, MAX_DUMPING_TIME);
                break;
                
            case DUMPING:
                currentStatus = VehicleStatus.ROUTING;
                stateDuration = randomInt(MIN_ROUTING_TIME, MAX_ROUTING_TIME);
                cycleCount++;
                LOG.debug("Vehicle {} completed cycle {}", vehicleId, cycleCount);
                break;
                
            default:
                currentStatus = VehicleStatus.IDLE;
                stateDuration = randomInt(MIN_IDLE_TIME, MAX_IDLE_TIME);
        }
        
        LOG.info("Vehicle {} transitioned: {} -> {} (duration: {}s)", 
                vehicleId, previousStatus, currentStatus, stateDuration);
    }
    
    public String getVehicleId() {
        return vehicleId;
    }
    
    public VehicleStatus getCurrentStatus() {
        return currentStatus;
    }
    
    public int getCycleCount() {
        return cycleCount;
    }
    
    public String getStateInfo() {
        long elapsedSeconds = (System.currentTimeMillis() - stateStartTime) / 1000;
        long remainingSeconds = stateDuration - elapsedSeconds;
        return String.format("%s [%s] - Cycle: %d, Remaining: %ds", 
                vehicleId, currentStatus, cycleCount, remainingSeconds);
    }
    
    private int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
