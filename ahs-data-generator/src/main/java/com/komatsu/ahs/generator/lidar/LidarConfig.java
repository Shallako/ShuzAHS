package com.komatsu.ahs.generator.lidar;

import com.jme3.math.Vector3f;

public class LidarConfig {
    // Field of view in degrees
    private float horizontalFovDeg = 360f;
    private float verticalFovDeg = 10f;

    // Samples (resolution)
    private int horizontalSamples = 1440; // 0.25° resolution
    private int verticalSamples = 1;      // 2D planar by default

    // Max range in meters
    private float maxRange = 120f;

    // Sensor position offset relative to vehicle origin (meters)
    private Vector3f sensorOffset = new Vector3f(0, 2.0f, 0);

    public float getHorizontalFovDeg() {
        return horizontalFovDeg;
    }

    public void setHorizontalFovDeg(float horizontalFovDeg) {
        this.horizontalFovDeg = horizontalFovDeg;
    }

    public float getVerticalFovDeg() {
        return verticalFovDeg;
    }

    public void setVerticalFovDeg(float verticalFovDeg) {
        this.verticalFovDeg = verticalFovDeg;
    }

    public int getHorizontalSamples() {
        return horizontalSamples;
    }

    public void setHorizontalSamples(int horizontalSamples) {
        this.horizontalSamples = horizontalSamples;
    }

    public int getVerticalSamples() {
        return verticalSamples;
    }

    public void setVerticalSamples(int verticalSamples) {
        this.verticalSamples = verticalSamples;
    }

    public float getMaxRange() {
        return maxRange;
    }

    public void setMaxRange(float maxRange) {
        this.maxRange = maxRange;
    }

    public Vector3f getSensorOffset() {
        return sensorOffset;
    }

    public void setSensorOffset(Vector3f sensorOffset) {
        this.sensorOffset = sensorOffset;
    }
}
