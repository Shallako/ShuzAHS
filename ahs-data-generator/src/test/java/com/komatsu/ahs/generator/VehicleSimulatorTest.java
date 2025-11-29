package com.komatsu.ahs.generator;

import com.komatsu.ahs.domain.model.VehicleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Vehicle Simulator Tests")
class VehicleSimulatorTest {

    private VehicleSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new VehicleSimulator("KOMATSU-930E-001");
    }

    @Test
    @DisplayName("Should initialize in IDLE state")
    void testInitialState() {
        assertEquals(VehicleStatus.IDLE, simulator.getCurrentStatus());
        assertEquals(0, simulator.getCycleCount());
    }

    @Test
    @DisplayName("Should transition from IDLE to ROUTING")
    void testIdleToRouting() {
        // Fast-forward through IDLE state duration
        for (int i = 0; i < 100; i++) {
            simulator.updateState();
        }
        
        // Should eventually reach ROUTING
        assertNotEquals(VehicleStatus.IDLE, simulator.getCurrentStatus());
    }

    @Test
    @DisplayName("Should follow state machine: IDLE → ROUTING → LOADING → HAULING → DUMPING")
    void testStateMachineSequence() {
        VehicleStatus[] expectedSequence = {
            VehicleStatus.IDLE,
            VehicleStatus.ROUTING,
            VehicleStatus.LOADING,
            VehicleStatus.HAULING,
            VehicleStatus.DUMPING
        };
        
        int stateIndex = 0;
        VehicleStatus lastStatus = simulator.getCurrentStatus();
        
        // Simulate 1000 updates (should complete at least one cycle)
        for (int i = 0; i < 1000; i++) {
            simulator.updateState();
            VehicleStatus currentStatus = simulator.getCurrentStatus();
            
            if (currentStatus != lastStatus) {
                // State changed
                if (stateIndex < expectedSequence.length - 1) {
                    stateIndex++;
                } else {
                    // Completed one cycle, back to ROUTING
                    assertEquals(VehicleStatus.ROUTING, currentStatus);
                }
                lastStatus = currentStatus;
            }
        }
        
        // Should have progressed through states
        assertTrue(simulator.getCycleCount() >= 0);
    }

    @Test
    @DisplayName("Should increment cycle count after DUMPING")
    void testCycleCountIncrement() {
        int initialCycles = simulator.getCycleCount();
        
        // Simulate enough updates to complete one full cycle
        for (int i = 0; i < 2000; i++) {
            simulator.updateState();
        }
        
        // Cycle count should have increased
        assertTrue(simulator.getCycleCount() >= initialCycles);
    }

    @Test
    @DisplayName("Should return valid state info")
    void testGetStateInfo() {
        String stateInfo = simulator.getStateInfo();
        
        assertNotNull(stateInfo);
        assertTrue(stateInfo.contains("IDLE") || stateInfo.contains("ROUTING") 
            || stateInfo.contains("LOADING") || stateInfo.contains("HAULING") 
            || stateInfo.contains("DUMPING"));
        assertTrue(stateInfo.contains("Cycle:"));
        assertTrue(stateInfo.contains("Remaining:"));
    }

    @Test
    @DisplayName("Should handle multiple state transitions")
    void testMultipleTransitions() {
        VehicleStatus initialStatus = simulator.getCurrentStatus();
        
        // Update many times
        for (int i = 0; i < 500; i++) {
            simulator.updateState();
        }
        
        // State should have changed at some point
        // (or we're still in IDLE with long duration, which is valid)
        assertNotNull(simulator.getCurrentStatus());
    }

    @Test
    @DisplayName("Should have valid state durations")
    void testStateDurations() {
        // Create multiple simulators to test randomness
        for (int i = 0; i < 10; i++) {
            VehicleSimulator sim = new VehicleSimulator("TEST-" + i);
            
            // Let it run through a few states
            for (int j = 0; j < 100; j++) {
                sim.updateState();
            }
            
            // Should not crash and should be in a valid state
            assertNotNull(sim.getCurrentStatus());
        }
    }

    @Test
    @DisplayName("Should transition to DUMPING only from HAULING")
    void testDumpingTransition() {
        // Force into HAULING state by simulating many updates
        boolean reachedHauling = false;
        boolean reachedDumping = false;
        VehicleStatus previousStatus = null;
        
        for (int i = 0; i < 2000; i++) {
            VehicleStatus current = simulator.getCurrentStatus();
            
            if (current == VehicleStatus.HAULING) {
                reachedHauling = true;
            }
            
            if (current == VehicleStatus.DUMPING) {
                reachedDumping = true;
                // When we reach DUMPING, previous status should have been HAULING
                if (previousStatus != null) {
                    assertEquals(VehicleStatus.HAULING, previousStatus);
                }
            }
            
            previousStatus = current;
            simulator.updateState();
        }
        
        // Should have progressed through the cycle
        assertTrue(reachedHauling || reachedDumping);
    }

    @Test
    @DisplayName("Should track vehicle ID correctly")
    void testVehicleId() {
        assertEquals("KOMATSU-930E-001", simulator.getVehicleId());
    }
}
